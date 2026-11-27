# Hacking Guide for the automatically generated [nbjavac](README.md)

The idea of the `nb-javac` build system is to take the JDK `javac` sources
from its **official repository** and
**automatically convert** them to run on _older JDKs_ (as old as JDK 8 right now).
The `javac` sources come from the OpenJDK checkout in `jdk/`. Make compiler
bugfixes and features there and integrate them into the official JDK repository;
do not edit the generated copies under `target/`.

The repository's own non-test Java sources used by nb-javac are the compatibility
helpers in [`runtime/emulation-jdk8/src/main/java`](runtime/emulation-jdk8/src/main/java)
and [`runtime/emulation-jdk21/src/main/java`](runtime/emulation-jdk21/src/main/java).
Their [Jackpot refactorings](https://netbeans.apache.org/jackpot/HintsFileFormat.html)
are in each project's `src/main/resources/META-INF/upgrade/` directory. Runtime
tests are in [`runtime/run-on-jdk8/src/test`](runtime/run-on-jdk8/src/test).
The repository also contains Java build tools under `make/langtools/tools/`.

```bash
$ JAVA_HOME=/jdk-25/ mvn clean package
```

The reactor builds separate JDK 8+ and JDK 21+ implementations with their own
generated sources and output directories. A regular `mvn clean` retains the
OpenJDK checkout.

Use the above command to build everything at once. The build is driven by
the standard Maven lifecycle phases:

| Phase | What it does |
| ----- | ------------ |
| `initialize` | checks the OpenJDK sources out into `jdk` (see below) |
| `generate-sources` | applies the automatic refactorings to the sources (see below) |
| `compile` | compiles the sources to the appropriate target Java bytecode |
| `test` | compiles and runs the tests with `surefire` (skip with `-DskipTests`, run one with `-Dtest=StringWrapperTest`) |
| `package` | creates the shared API, both implementation and sources jars, and checks their consistency |
| `verify` | with the `release` profile signs the artifacts (see the [README](README.md)) |

The root `pom.xml` aggregates `ctsym`, `api`, `runtime`, and `code`.
The `runtime` project aggregates `emulation-jdk8`, `emulation-jdk21`,
`run-on-jdk8` and `run-on-jdk21`. The latter two compile and test the
implementations; `code` retains the
`com.dukescript.nbjavac:nb-javac` artifact and assembles their JARs in
`code/target`.

The `ctsym` module repackages `META-INF/ct*` data from `org.frgaal:compiler`
into `ctsym/target/nb-javac-ctsym-<version>.jar`. Both runtime modules unpack
that JAR into their implementations. The `api` module builds the shared JDK
8-compatible API, which `code` attaches as `nb-javac:jar:api`. When building
only `code`, use `mvn -pl code -am ...` to build its prerequisites.

Any `-Dkey=value` given to `mvn` is passed to the build. Read below to control individual steps of the build.


### Getting the JDK repository

The build requires JDK repository in `jdk` subdirectory of the root of this `nb-javac` repository.
If such directory doesn't exist, the build checks one out. That's done by the
_initialization step_:

```bash
$ JAVA_HOME=/jdk-25/ mvn initialize
```

The _initialization step_ needs to know the _exact commit_ to checkout. Also
it needs to know the OpenJDK like repository to check the sources from.
Default values of these `jdk.git.url` and `jdk.git.commit` properties
are provided in the [pom.xml](pom.xml) file. The version of the produced artifacts
(`com.dukescript.nbjavac:nb-javac`) defaults to the `jdk.git.commit` and can be changed
with `-Drevision=...`.
Those default values can be optionally overriden from a command line:

```bash
$ JAVA_HOME=/jdk-25/ mvn initialize \
    -Djdk.git.url=https://github.com/openjdk/jdk17 \
    -Djdk.git.commit=jdk-17+35
```

If the `jdk` directory is present the build _leaves its content untouched_. E.g.
a developer may clone the `jdk` repository manually, switch its content to any other tag,
make changes in the `jdk/src/java.compiler/` or `jdk/src/jdk.compiler/` directories,
etc.

One can discard any changes by `mvn clean -Dclean.jdk=true`, which removes the `jdk` directory
(a regular `mvn clean` leaves it alone). Then the subsequent build checks
a fresh copy of the `jdk` repository from scratch.

### Automatically processing the sources

Once the JDK's `javac` sources are in the `jdk` subdirectory, `run-on-jdk8`
combines the [JDK 21+ refactorings](./runtime/emulation-jdk21/src/main/resources/META-INF/upgrade/nbjavac-jdk21.hint)
with the additional [JDK 8+ compatibility refactorings](./runtime/emulation-jdk8/src/main/resources/META-INF/upgrade/nbjavac.hint).
`run-on-jdk21` applies only the shared set. The
[Jackpot](https://netbeans.apache.org/jackpot/HintsFileFormat.html) phase
generates both:

```bash
$ JAVA_HOME=/jdk-25/ mvn generate-sources
```

This step copies and transforms the OpenJDK sources separately for each project:

- API: `api/target/generated/java/javac-api` and
  `api/target/generated/java/javac-impl` contain the API sources, including
  `javax.tools` and `com.sun.source`, respectively.
- Runtime implementations: `runtime/run-on-jdk8/target/generated/java` and
  `runtime/run-on-jdk21/target/generated/java` contain their respective
  transformed sources.

After generation, the API project exposes these source roots in NetBeans as
`Generated Sources (javac-api)` and `Generated Sources (javac-impl)`.
Each `jackpot-done` marker records the profile, OpenJDK
commit, and hint checksum. The emulation projects own the helper sources and
publish source JARs for the runtime projects. The JDK 8 runtime uses both
emulation projects; the JDK 21 runtime uses only `emulation-jdk21`.
`run-on-jdk8` owns the shared runtime tests and publishes their sources for
`run-on-jdk21` to compile and run, except for JDK 8 emulation-specific tests.

Generated sources shall not be edited manually. Instead, edit
the sources in the original
`jdk/src/java.compiler/` and `jdk/src/jdk.compiler/` directories. To apply
the refactorings again execute:

```bash
$ JAVA_HOME=/jdk-25/ mvn clean generate-sources
```

### The build

As described in [general documentation](README.md) use the following command to
generate the final JAR files:

```bash
$ JAVA_HOME=/jdk-25/ mvn clean package
```

The build produces `nb-javac-<version>.jar`,
`nb-javac-<version>-jdk21.jar`, a shared `nb-javac-<version>-api.jar`,
and `sources` and `jdk21-sources` JARs in `./code/target/`. All are
installed under the original `nb-javac` coordinates.

### Maven repository staging in CI

After both implementations pass their runtime tests, GitHub Actions
uploads a `maven-repository-<run-id>` artifact with a Maven-layout directory
tree. The unclassified artifacts are the `jdk8+` build; the `jdk21+`
implementation and sources have `jdk21` and `jdk21-sources` classifiers.
The `jdk8+` multi-release API JAR is staged once with the `api` classifier,
for both implementations.

The API JAR is shared because the public and protected signatures are the same
across profiles, and most API types are interfaces or simple classes with
little implementation. The small differences are handled by a few
multi-release overrides, including `SourceVersion` for JDK 11+ and `Modifier`
for JDK 17+.

The implementation JARs remain separate so `jdk21+` can target Java 21 and
exclude the older compatibility classes. This keeps the JDK 21+ artifact
smaller and free of `jdk8+` profile output.

The staged tree is an inspection artifact, not a signed Maven Central
publication bundle. A `SHA256SUMS` manifest at the repository root is checked
before upload; after downloading and extracting the artifact, verify it with
`cd maven-repository && sha256sum -c SHA256SUMS`.

### Debug & Develop

Open the `nb-javac` project (a regular Maven project) in NetBeans IDE with

```bash
$ netbeans --open .
```

and you should be able to debug a test (for example `StringWrapperTest`) with following command line:

```bash
$ JAVA_HOME=/jdk-8/ mvn -pl runtime/run-on-jdk8 surefire:test -Dtest=StringWrapperTest -Dmaven.surefire.debug
```

The test JVM waits for a debugger on port 5005.
Connect the NetBeans IDE to port 5005 and step through the `nb-javac`
generated code.
