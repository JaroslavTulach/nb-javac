# Run the Latest Java Compiler on old JDKs!

_nb-javac_ is a backport of OpenJDK "javac", i.e., the Java compiler. _nbjavac_
takes sources from the latest JDK and backports them to run on JDK8+.
It allows to run the latest OpenJDK compiler on older JDKs - as old
as JDK8 while being 1:1 identical with the official `javac`.
Use `nb-javac` to execute the latest Java language compiler on old
JDKs!

## Prerequisite
  - Git
  - Apache Maven
  - JDK 25 to build
  - older JDKs to test compatibility

## Building nb-javac jar files

Detailed description of the [build process](BUILD.md) is available in a
separate [development documentation](BUILD.md). Here are just
the most straigtforward steps to get the final artifacts.

### Obtain the code with the following command

```
$ git clone https://github.com/jaroslavtulach/nb-javac
```

### Get a specific version use the following command

```bash
$ git checkout <release_tag_name> 
```

### Build the nb-javac project

```bash
$ JAVA_HOME=/jdk-25/ mvn clean package
```

By default this project enables `jdk8+` compatibility profile. Such a profile guarantees the
generated `nb-javac` can run on JDK8+, but in order to do so, it needs to apply more tweaks
to the original OpenJDK sources. If you are not interested in such a level of portability,
feel free to use `-Pjdk21+` profile to select only the
refactorings needed for JDK 21 and later.

The profiles share the Maven output directory. Run `mvn clean` when switching
profiles so the next artifact cannot contain classes from the previous build.

```bash
$ JAVA_HOME=/jdk-25/ mvn clean package -Pjdk21+
```

Two jars namely `nb-javac-*-api.jar` and `nb-javac-*.jar` (the implementation)
are going to appear in the `./target/` directory. It is also possible to
sanity test the generated Javac on JDK8 (in case of `jdk8+` profile being used):
```bash
$ JAVA_HOME=/jdk-8/ mvn surefire:test
```
The `package` phase also creates a sources JAR: `nb-javac-*-sources.jar`
for `jdk8+`, or `nb-javac-*-jdk21-sources.jar` for `jdk21+`.

There is one shared API JAR and two implementation JARs: one for `jdk8+` and
one for `jdk21+`.

# Publishing to Maven Central

The artifacts use `com.dukescript.nbjavac:nb-javac` and the version defaults
to `jdk.git.commit` (see [BUILD.md](BUILD.md)). To publish both implementation
profiles together:

1. Get an account at the [Central Portal](https://central.sonatype.com/) and
   register the `com.dukescript.nbjavac` namespace. Make a GPG signing key
   available and publish its public key as described in
   [Central's signing instructions](https://central.sonatype.org/publish/requirements/gpg/).
   The [maven-gpg-plugin](https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html)
   selects the key, for example using `-Dgpg.keyname=...`.

2. With JDK 25, install and sign both profiles, in this order:
   ```bash
   JAVA_HOME=/jdk-25/ mvn clean install -Prelease
   JAVA_HOME=/jdk-25/ mvn clean install -Prelease,jdk21+
   ```
   Use the same version and signing key for both commands. The first installs
   the JDK 8+ implementation, shared API, sources, Javadoc and POM. The second
   adds the `jdk21` implementation and `jdk21-sources` without replacing the
   shared API, whose JDK 8+ build is needed for older runtimes. Both commands
   also install signatures; neither uploads anything.

3. Package the installed files as a Central bundle. This example uses the
   default local Maven repository; change the directory if necessary. If
   `revision` was overridden in step 2, use the same override when resolving
   `VERSION`. The commands verify the signatures and write Central's required
   MD5 and SHA-1 checksums into the local repository.
   ```bash
   $ cd ~/.m2/repository
   repository$ zip -r "$HOME/central-bundle.zip" "com/dukescript/nbjavac/nb-javac-jdk28*"
   ```
   The ZIP command refuses to overwrite an existing bundle. Only the selected
   release files are included, not cached dependencies or local metadata.

4. Upload `~/central-bundle.zip` through the
   [Central Portal](https://central.sonatype.org/publish/publish-portal-upload/),
   wait for validation, and release the deployment in the portal.

# Install into a NetBeans distribution

To build nb-javac and install its API and implementation jars into an existing
NetBeans distribution, run this from the nb-javac checkout:

```bash
JAVA_HOME=/jdk-25/ mvn install \
    -Pjdk21+ \
    -Dnetbeans.dest.dir=/path/to/netbeans/nbbuild/netbeans
```

Setting `netbeans.dest.dir` activates the install profile. It replaces the
existing nb-javac jars in the distribution's `java/modules/ext` directory,
preserving the filenames expected by its modules, then touches the Java
cluster's `.lastModified` marker to invalidate NetBeans' caches. The build
fails if the destination is not a NetBeans distribution with exactly one
nb-javac API jar and one implementation jar. Restart NetBeans to load the
updated jars.

# Documentation 

- https://cwiki.apache.org/confluence/display/NETBEANS/Overview%3A+nb-javac
- https://cwiki.apache.org/confluence/display/NETBEANS/Release+Schedule

# Help
- Email jaroslav.tulach@apidesign.org - the NetBeans founder and maintainer of `nb-javac`.
- Subscribe or mail the dev@netbeans.apache.org list - Join development discussions, propose new ideas and connect with contributors.

# Contributing
See the  [Contributing Policy](./CONTRIBUTING.md)

# Security
See the  [Security Policy](./SECURITY.md)
