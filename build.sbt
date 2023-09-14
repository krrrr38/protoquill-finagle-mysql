val scala3Version = "3.3.1"

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "protoquill-finagle-mysql",
    organization := "com.krrrr38",
    versionScheme := Some("early-semver"),
    homepage := Some(
      url("https://github.com/krrrr38/protoquill-finagle-mysql")
    ),
    licenses := List(
      ("Apache License 2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
    ),
    developers := List(
      Developer(
        "krrrr38",
        "Ken Kaizu",
        "k.kaizu38@gmail.com",
        url("https://krrrr38.com")
      )
    ),
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-sql" % "4.6.0.1",
      ("com.twitter" %% "finagle-mysql" % "22.12.0" cross CrossVersion.for3Use2_13)
        .exclude("org.scala-lang.modules", "scala-collection-compat_2.13"),
      "org.scalameta" %% "munit" % "1.0.0-M9" % Test,
      "org.scalameta" %% "munit-scalacheck" % "1.0.0-M9" % Test,
      "ch.qos.logback" % "logback-classic" % "1.4.11" % Test
    ),
    Test / parallelExecution := false
  )
