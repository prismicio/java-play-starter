import play.twirl.sbt.Import._

name := "prismicio-starter"

version := "2.0"

scalaVersion := "2.11.6"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  javaWs,
  "io.prismic" % "java-kit" % "1.3.2",
  "org.mockito" % "mockito-core" % "1.10.19"
)

TwirlKeys.templateImports += "prismic.Context._"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

routesGenerator := InjectedRoutesGenerator
