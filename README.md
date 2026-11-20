# Run Latest Java Compiler on old JDKs!

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
  - JDK 8 to test

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

### Run the below command to build nb-javac.

```bash
$ JAVA_HOME=/jdk-25/ mvn clean package -DskipTests
```

Two jars namely `nb-javac-*-api.jar` and `nb-javac-*.jar` (the implementation)
are going to appear in the `./target/` directory. It is also possible to
sanity test the generated Javac on JDK8:
```bash
$ JAVA_HOME=/jdk-8/ mvn test
```

### Generate JAR with the source code of nb-javac

The `nb-javac-*-sources.jar` is created in the `./target/` directory by the `package` phase:

```bash
$ JAVA_HOME=/jdk-25/ mvn package -DskipTests
```

# Publishing to maven central

The artifacts (`com.dukescript.nbjavac:nb-javac`, the version is the `jdk.git.commit`, see [BUILD.md](BUILD.md))
are uploaded directly to the [Central Portal](https://central.sonatype.com/) by `mvn deploy`
with the `release` profile. The profile builds the Javadoc jar Central requires, signs the
artifacts (the main jar, the `api`, `sources` and `javadoc` jars and the pom) with `gpg` and
uploads them with the [central-publishing-maven-plugin](https://central.sonatype.org/publish/publish-portal-maven/).

1. Get an account at https://central.sonatype.com/, register the `com.dukescript.nbjavac` namespace
   and [generate a user token](https://central.sonatype.org/publish/generate-portal-token/).

2. Configure the maven installation so that the token is made available
   for the server with the id `central`, e.g. in `~/.m2/settings.xml`:
   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>your token username</username>
         <password>your token password</password>
       </server>
     </servers>
   </settings>
   ```

3. Make a `gpg` key available. The key is selected by the usual
   [maven-gpg-plugin](https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html)
   properties, e.g. `-Dgpg.keyname=...`.

4. Run
   ```
   JAVA_HOME=/jdk-25/ mvn clean deploy -DskipTests -Prelease
   ```
   The build uploads the bundle and waits until Central validates it. The deployment then
   has to be released manually at https://central.sonatype.com/publishing/deployments

To only create the bundle without uploading it (e.g. to upload it by other tools),
add `-Dcentral.skipUpload=true`. The bundle is then in `target/central-publishing/central-bundle.zip`.

# Installation/Usage

#### 1. Copy jars by following commands

```bash
cp nb-javac/target/nb-javac-$ver-api.jar netbeans/java/libs.javacapi/external/nb-javac-$ver-api.jar
cp nb-javac/target/nb-javac-$ver.jar netbeans/java/libs.javacimpl/external/nb-javac-$ver-impl.jar
```

#### 2. Open Netbeans and activate Java SE by creating or opening a ant, maven or gradle then copy jars by these commands

```bash
cp nb-javac/target/nb-javac-$ver.jar netbeans/nbbuild/testuserdir/modules/ext/nb-javac-$ver-impl.jar
cp nb-javac/target/nb-javac-$ver-api.jar netbeans/nbbuild/testuserdir/modules/ext/nb-javac-$ver-api.jar
touch netbeans/nbbuild/testuserdir/.lastmodified
```

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
