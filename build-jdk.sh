#/bin/bash
set -e

if [ "x$JAVA_HOME_VERSION" == "x" ]; then
  if ! [ -f $JAVA_HOME/bin/javac ]; then
    if ! [ -f $JAVA_HOME25/bin/javac ]; then
      echo Specify JAVA_HOME environment variable!
      exit 1
    fi
    JAVA_HOME_VERSION=$JAVA_HOME25
  else
    JAVA_HOME_VERSION=$JAVA_HOME
  fi
fi

echo Using Java at $JAVA_HOME_VERSION

if [ "x$JAVA_VERSION" == "x" ]; then
  $JAVA_HOME_VERSION/bin/java -version
  JAVA_VERSION=`$JAVA_HOME_VERSION/bin/java -version 2>&1 | grep version | head -n 1 | cut -f 2 -d \" | cut -f 1 -d - | cut -f 1 -d .`
  echo Detected Java version: $JAVA_VERSION
fi

if ! [ -f $JAVA_HOME_VERSION/bin/javac ]; then
  echo Specify JAVA_HOME_VERSION environment variable!
  exit 1
fi

rm -rf build/test
mkdir -p build/test
mkdir -p build/test/modules
cp $JAVA_HOME_VERSION/jmods/* build/test/modules
#clear ModuleHashes on java.base:
$JAVA_HOME_VERSION/bin/java --add-modules jdk.jdeps --add-exports jdk.jdeps/com.sun.tools.classfile=ALL-UNNAMED StripModuleHashes.java $JAVA_HOME_VERSION/jmods/java.base.jmod build/test/modules/java.base.jmod

#prepare:

#java.compiler:
rm build/test/modules/java.compiler.jmod

mkdir -p build/test/src/java.compiler

cp jdk/src/java.compiler/share/classes/module-info.java build/test/src/java.compiler

patch -R build/test/src/java.compiler/module-info.java temporary-patches/test-java.compiler

mkdir -p build/test/out/java.compiler
cp -r target/classes/javax build/test/out/java.compiler/
cp -r target/classes/nbjavac build/test/out/java.compiler/

#jdk.compiler:
rm build/test/modules/jdk.compiler.jmod

mkdir -p build/test/src/jdk.compiler
cp jdk/src/jdk.compiler/share/classes/module-info.java build/test/src/jdk.compiler

patch build/test/src/jdk.compiler/module-info.java temporary-patches/test-jdk.compiler

mkdir -p build/test/out/jdk.compiler
mkdir -p build/test/out/jdk.compiler/com/sun/source
cp -r target/classes/com/sun/source build/test/out/jdk.compiler/com/sun/
mkdir -p build/test/out/jdk.compiler/com/sun/tools/javac
cp -r target/classes/com/sun/tools/javac build/test/out/jdk.compiler/com/sun/tools/
mkdir -p build/test/out/jdk.compiler/com/sun/tools/doclint
cp -r target/classes/com/sun/tools/doclint build/test/out/jdk.compiler/com/sun/tools/

rm -rf build/test/expanded/jdk.compiler
mkdir -p build/test/expanded/jdk.compiler

$JAVA_HOME_VERSION/bin/jmod extract --dir=build/test/expanded/jdk.compiler $JAVA_HOME_VERSION/jmods/jdk.compiler.jmod

#jdk.jdeps:
rm build/test/modules/jdk.jdeps.jmod

mkdir -p build/test/src/jdk.jdeps

mkdir -p build/test/out/jdk.jdeps

rm -rf build/test/expanded/jdk.jdeps
mkdir -p build/test/expanded/jdk.jdeps

$JAVA_HOME_VERSION/bin/jmod extract --dir=build/test/expanded/jdk.jdeps $JAVA_HOME_VERSION/jmods/jdk.jdeps.jmod

cp -r build/test/expanded/jdk.jdeps/classes/* build/test/out/jdk.jdeps/

rm -rf build/test/out/jdk.jdeps/com/sun/tools/classfile/

mkdir -p build/test/out/jdk.jdeps/com/sun/tools/classfile/
cp -r target/classes/com/sun/tools/classfile build/test/out/jdk.jdeps/com/sun/tools/
mkdir -p build/test/out/jdk.jdeps/com/sun/tools/javap
cp -r target/classes/com/sun/tools/javap build/test/out/jdk.jdeps/com/sun/tools/

#build module-infos:
$JAVA_HOME_VERSION/bin/javac --module-source-path build/test/src/ -d build/test/out `find build/test/src/ -type f -name "*.java"`

#build jmods:
$JAVA_HOME_VERSION/bin/jmod create --class-path build/test/out/java.compiler/ --module-version $JAVA_VERSION build/test/modules/java.compiler.jmod
$JAVA_HOME_VERSION/bin/jmod create --class-path build/test/out/jdk.compiler/ --cmds build/test/expanded/jdk.compiler/bin/ --legal-notice build/test/expanded/jdk.compiler/legal/ --libs build/test/expanded/jdk.compiler/lib/ --man-pages build/test/expanded/jdk.compiler/man/ --module-version $JAVA_VERSION build/test/modules/jdk.compiler.jmod
$JAVA_HOME_VERSION/bin/jmod create --class-path build/test/out/jdk.jdeps/ --cmds build/test/expanded/jdk.jdeps/bin/ --legal-notice build/test/expanded/jdk.jdeps/legal/ --man-pages build/test/expanded/jdk.jdeps/man/ --module-version $JAVA_VERSION build/test/modules/jdk.jdeps.jmod

#build image:
$JAVA_HOME_VERSION/bin/jlink -p build/test/modules:$JAVA_HOME_VERSION/jmods/ --add-modules ALL-MODULE-PATH --output build/test/jdk

# test

echo "class Demo {" > build/test/Demo.java
echo "  public static void main(String[] args) {" >> build/test/Demo.java
echo '    System.out.println("Find patched Java version " + System.getProperty("java.version") + " at " + System.getProperty("java.home"));' >> build/test/Demo.java
echo "  }" >> build/test/Demo.java
echo "}" >> build/test/Demo.java

build/test/jdk/bin/javac --release 11 -d build/test/ build/test/Demo.java
build/test/jdk/bin/java -cp build/test/ Demo
