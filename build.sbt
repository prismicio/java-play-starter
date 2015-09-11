import play.twirl.sbt.Import._

name := "prismicio-starter"

version := "1.1"

scalaVersion := "2.11.6"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  javaWs,
  "io.prismic" % "java-kit" % "1.3.2"
)

TwirlKeys.templateImports += "controllers.Prismic._"

lazy val root = (project in file(".")).enablePlugins(PlayJava)
