# Hacking Guide for the automatically generated [nbjavac](README.md)

The idea of the `nb-javac` build system is to take the JDK `javac` sources
from its **official repository** and
**automatically convert** them to run on _older JDKs_ (as old as JDK 8 right now).
As a result:
- the sources come from real JDK repository.
- this `nbjavac` repository doesn't contain sources.

This repository only contains the build scripts and
description of [advanced refactorings](https://netbeans.apache.org/jackpot/HintsFileFormat.html).
Bugfixes, features and other changes to `javac` sources are supposed to be done
in the `jdk` subdirectory and integrated into the JDK's `javac` **official repository**.

```bash
$ JAVA_HOME=/jdk-25/ mvn package -DskipTests
```

Use the above command to build everything at once. The build is driven by
the standard Maven lifecycle phases:

| Phase | What it does |
| ----- | ------------ |
| `initialize` | checks the OpenJDK sources out into `jdk` (see below) |
| `generate-sources` | applies the automatic refactorings to the sources (see below) |
| `compile` | compiles the sources with the [Frgaal](https://frgaal.org) compiler into Java 8 bytecode (and the `SourceVersion` for Java 11) |
| `test` | compiles and runs the tests with `surefire` (skip with `-DskipTests`, run one with `-Dtest=StringWrapperTest`) |
| `package` | creates the `api` and implementation jars and the sources zip |
| `deploy` | with the `release` profile signs and deploys the artifacts (see the [README](README.md)) |

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

Once the JDK's `javac` sources are in the `jdk` subdirectory, it is necessary
to apply [advanced refactorings](./make/langtools/netbeans/nb-javac/src/META-INF/upgrade/nbjavac.hint)
to them. This is done by executing the [jackpot](https://netbeans.apache.org/jackpot/HintsFileFormat.html)
phase:

```bash
$ JAVA_HOME=/jdk-25/ mvn generate-sources
```

This step copies the `javac` sources from the `jdk` subdirectory into a sibling
`src` subdirectory and applies necessary transformations to them.
The goal of such transformations is to eliminate usage of JDK9+ APIs
and replace them with JDK8 only APIs.

The sources under the `src/java.compiler` and `src/jdk.compiler` shall not
be edited manually. Rather than that edit the sources in the original
`jdk/src/java.compiler/` and `jdk/src/jdk.compiler/` directories. To apply
the refactorings again execute:

```bash
$ JAVA_HOME=/jdk-25/ mvn clean generate-sources
```

### The build

As described in [general documentation](README.md) use the following command to
generate the final JAR files:

```bash
$ JAVA_HOME=/jdk-25/ mvn clean package -DskipTests
```

JARs `nb-javac-*.jar` (the implementation) and `nb-javac-*-api.jar` are going to appear
in the `./target/` directory.

### Debug & Develop

Open the `nb-javac` project (a regular Maven project) in NetBeans IDE with

```bash
$ netbeans --open .
```

and you should be able to debug a test (for example `StringWrapperTest`) with following command line:

```bash
$ JAVA_HOME=/jdk-8/ mvn test -Dtest=StringWrapperTest -Dmaven.surefire.debug
```

The test JVM waits for a debugger on port 5005.
Connect the NetBeans IDE to port 5005 and step through the `nb-javac`
generated code.
