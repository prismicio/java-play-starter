import play.twirl.sbt.Import._

name := "prismicio-starter"

version := "1.2"

scalaVersion := "2.11.6"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  javaWs,
  "io.prismic" % "java-kit" % "1.4.0"
)

TwirlKeys.templateImports += "prismic._"

routesGenerator := InjectedRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(PlayJava)
