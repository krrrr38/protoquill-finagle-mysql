val scala3Version = "3.2.2"

addCommandAlias("fmt", "all scalafmt test:scalafmt")

lazy val root = project
  .in(file("."))
  .settings(
    name := "protoquill-finagle-mysql",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-sql" % "4.6.0.1",
      ("com.twitter" %% "finagle-mysql" % "22.12.0" cross CrossVersion.for3Use2_13)
        .exclude("org.scala-lang.modules", "scala-collection-compat_2.13"),
      "org.scalameta" %% "munit" % "1.0.0-M7" % Test,
      "org.scalameta" %% "munit-scalacheck" % "1.0.0-M7" % Test,
    ),
    Test / parallelExecution := false,
  )
