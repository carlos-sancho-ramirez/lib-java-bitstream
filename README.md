# lib-java-bitstream

Allow serialising data through Java streams in an optimised way.

## Objective

Java streams is an interface that allow the developers reading and writing from different resources (memory, disk or network) just managing instances of java.io.InputStream or java.io.OutputStream. Once the stream instance is created a program can be created to read or write to any kind of resource. However, these streams are thought as byte per byte streams. As a consequence, when an array of booleans has to be serialised into the stream, it can take much more space than required. As booleans can be representated using a single bit, instead of the 8 that a byte has, it may take up to 8 times more.

This library provides a wrapper for an InputStream instance for reading purposes and a wrapper for an OutputStream instance for writing purposes. These wrappers allow serialising booleans instead of bytes composing a byte after calling the write boolean method 8 times or reading a single byte from the wrapped InputStream after calling 8 times to read boolean.

Once sending bits (or booleans) through a stream is possible, there is a bunch of optimizations that can be performed for more sophisticated data. Such the use of Huffman tables to encode with less bits the more frequent values and with more bits the less frequent ones. It allows compressing the data before sending it through the stream. For this reason, a bunch of other methods are present in the library in order the data can be optimized properly.

## How to build

### Using Gradle

This project include a basic build script for [Gradle](http://www.gradle.org/). That script includes the *maven-publish* plugin that allow publishing to Maven repositories. Consider running the *publishToMavenLocal* Gradle task.

    ./gradlew publishToMavenLocal

It will generate the artifact in a local maven repository (.m2 folder) located under the user space. Once published it can be used in other Gradle projects by importing the local maven repository and adding the proper artifact reference.

    repositories {
        mavenLocal()
    }
    
    dependencies {
        compile 'sword:bit-streams-library:1.0'
    }

### Using SBT

This project is configured to use the [Simple Build Tool](http://www.scala-sbt.org/). The following are some of the most valuable tasks you may execute:
  * *package*

    Compile all sources and pack them in a JAR file. This file should be found at ./target/scala-2.12 directory. This JAR is ready to use as a library and can be copied or linked as a dependency in your project.

  * *publishLocal*

    This task will follow the Maven stardard to generate all JAR, one for binaries, other for sources and other for the JavaDoc. This JAR will be included in the local copy of the Maven repository as it was a managed dependency downloaded from the Internet. This option will allow you to import this library using the maven standards and build tools like Maven, SBT or Gradle. The generated package may be addressed by:
      + organizationId: sword
      + artifactId: bit-streams-library_2.12
      + version: 1.0

    In case of being using SBT and compiling with Scala version 2.12.X, it can be included like:

        libraryDependencies += "sword" %% "bit-streams-library" % "1.0"
