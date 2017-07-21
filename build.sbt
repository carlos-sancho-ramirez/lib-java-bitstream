name := "Bit Streams library"

// This should be something like a domain name like "com.example"
organization := "sword"

description := "Allow serialising data through Java streams in an optimised way"

version := "1.0"

scalaVersion := "2.12.2"

publishMavenStyle := true

// This avoids including any Scala library as dependency. Thus a pure Java package can be generated
autoScalaLibrary := false

libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

