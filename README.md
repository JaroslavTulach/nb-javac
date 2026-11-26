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

One build creates a shared `nb-javac-*-api.jar`, the JDK 8+ implementation
`nb-javac-*.jar`, the JDK 21+ implementation `nb-javac-*-jdk21.jar`, and
their sources JARs in `./code/target/`.

To test the prebuilt implementations on their target JDKs:
```bash
$ JAVA_HOME=/jdk-8/ mvn -pl run-on-jdk8 surefire:test
$ JAVA_HOME=/jdk-21/ mvn -pl run-on-jdk21 surefire:test
```

# Publishing to Maven Central

The artifacts use `com.dukescript.nbjavac:nb-javac` and the version defaults
to `jdk.git.commit` (see [BUILD.md](BUILD.md)). To publish both implementation
implementations together:

1. Get an account at the [Central Portal](https://central.sonatype.com/) and
   register the `com.dukescript.nbjavac` namespace. Make a GPG signing key
   available and publish its public key as described in
   [Central's signing instructions](https://central.sonatype.org/publish/requirements/gpg/).
   The [maven-gpg-plugin](https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html)
   selects the key, for example using `-Dgpg.keyname=...`.

2. With JDK 25, install and sign both implementations:
   ```bash
   JAVA_HOME=/jdk-25/ mvn clean install -Prelease
   ```
   This installs the JDK 8+ and `jdk21` implementations, shared API, both
   sources JARs, Javadoc and POM. It also installs signatures and generates
   MD5 and SHA-1 checksums; it does not upload anything.

3. Package the installed files as a Central bundle. This example uses the
   default local Maven repository and version `jdk-28+25`; adjust them if
   necessary. Use a new ZIP filename, as `zip` updates an existing archive.
   ```bash
   cd ~/.m2/repository
   zip -r "$HOME/central-bundle.zip" com/dukescript/nbjavac/nb-javac/jdk-28+25
   ```
   Only the selected release is included, not cached dependencies or local
   repository metadata.

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
