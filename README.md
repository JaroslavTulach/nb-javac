# Run Latest Java Compiler on old JDKs!
_nb-javac_ is a backport of OpenJDK "javac", i.e., the Java compiler. _nbjavac_
takes sources from the latest JDK and backports them to run on JDK8+.
It allows to run the latest OpenJDK compiler on older JDKs - as old
as JDK8 while being 1:1 identical with the official `javac`.
Use `nb-javac` to execute the latest Java language compiler on old
JDKs!

## Prerequisite
  - Git
  - Ant 1.9.9 or above
  - JDK 17 to build
  - JDK 8 to test
  - Apache Maven to publish to Maven central

## Building nb-javac jar files

Detailed description of the [build process](BUILD.md) is available in a
separate [development documentation](BUILD.md). Here are just
the most straigtforward steps to get the final artifacts.

### Obtain the code with the following command

```
$ git clone https://github.com/oracle/nb-javac.git
```

### Get a specific version use the following command

```bash
$ git checkout <release_tag_name> 
```

### Run the below command to build nb-javac.

```bash
$ JAVA_HOME=/jdk-17/ ant -f ./make/langtools/netbeans/nb-javac clean jar
```

Two jars namely `nb-javac-*-api.jar` and `nb-javac-*-impl.jar` are going to appear
at location `./make/langtools/netbeans/nb-javac/dist/`. It is also possible to
sanity test the generated Javac on JDK8:
```bash
$ JAVA_HOME=/jdk-8/ ant -f ./make/langtools/netbeans/nb-javac test
```

### Generate ZIP with the source code of nb-javac

```bash
$ JAVA_HOME=/jdk-17/ ant -f ./make/langtools/netbeans/nb-javac zip-nb-javac-sources
```

# Publishing to maven central / OSSRH

1. Aquire an account for OSSRH from sonatype and get access to the target groupId
   See here: https://central.sonatype.org/pages/ossrh-guide.html

2. Configure the maven installation so that the credentials are made available
   for the server with the id oss.sonatype.org

3. Run
   ```
   JAVA_HOME=/jdk-17/ ant -f ./make/langtools/netbeans/nb-javac publish-to-ossrh-snapshots -Dmaven.groupId=your.grp.id
   ```
   to publish snapshot artifacts (https://oss.sonatype.org/content/repositories/snapshots/)

4. Run
   ```
   JAVA_HOME=/jdk-17/ ant -f ./make/langtools/netbeans/nb-javac publish-to-maven-central -Dmaven.groupId=your.grp.id
   ```
   to stage the release, which will get promoted to maven central, after it has
   been manually released.

# Installation/Usage

#### 1. Copy jars by following commands

```bash
cp nb-javac/make/langtools/netbeans/nb-javac/dist/nb-javac-$ver-api.jar netbeans/java/libs.javacapi/external/nb-javac-$ver-api.jar
cp nb-javac/make/langtools/netbeans/nb-javac/dist/nb-javac-$ver-impl.jar netbeans/java/libs.javacimpl/external/nb-javac-$ver-impl.jar
```

#### 2. Open Netbeans and activate Java SE by creating or opening a ant, maven or gradle then copy jars by these commands

```bash
cp nb-javac/make/langtools/netbeans/nb-javac/dist/nb-javac-$ver-impl.jar netbeans/nbbuild/testuserdir/modules/ext/nb-javac-$ver-impl.jar
cp nb-javac/make/langtools/netbeans/nb-javac/dist/nb-javac-$ver-api.jar netbeans/nbbuild/testuserdir/modules/ext/nb-javac-$ver-api.jar
touch netbeans/nbbuild/testuserdir/.lastmodified
```

# Documentation 

- https://cwiki.apache.org/confluence/display/NETBEANS/Overview%3A+nb-javac
- https://cwiki.apache.org/confluence/display/NETBEANS/Release+Schedule
- https://wiki.se.oracle.com/display/JPG/Behavior+without+NB-Javac

# Help
- Email jaroslav.tulach@apidesign.org - the NetBeans founder and maintainer of `nb-javac`.
- Subscribe or mail the dev@netbeans.apache.org list - Join development discussions, propose new ideas and connect with contributors.

# Contributing
See the  [Contributing Policy](./CONTRIBUTING.md)

# Security
See the  [Security Policy](./SECURITY.md)
