import play.twirl.sbt.Import._

name := "prismicio-starter"

version := "1.1"

scalaVersion := "2.11.1"

libraryDependencies += "io.prismic" % "java-kit" % "1.0.4"

libraryDependencies += javaWs

TwirlKeys.templateImports += "controllers.Prismic._"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

