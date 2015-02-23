import play.twirl.sbt.Import._

name := "prismicio-starter"

version := "1.1"

scalaVersion := "2.11.1"

resolvers += Resolver.mavenLocal

libraryDependencies += "io.prismic" % "java-kit" % "1.2.1"

libraryDependencies += javaWs

TwirlKeys.templateImports += "controllers.Prismic._"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

