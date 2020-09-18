ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "example"
ThisBuild / organizationName := "example"

val filterConsoleScalacOptions = { options: Seq[String] =>
  options.filterNot(Set(
    "-Xfatal-warnings",
    "-Werror",
    "-Wdead-code",
    "-Wunused:imports",
    "-Ywarn-unused:imports",
    "-Ywarn-unused-import",
    "-Ywarn-dead-code",
  ))
}

lazy val root = (project in file("."))
  .settings(
    name := "zio-presentation",
    scalacOptions := Seq(
      // Feature options
      "-encoding", "utf-8",
      "-explaintypes",
      "-feature",
      "-language:existentials",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Ymacro-annotations",
      // Warnings as errors!
      "-Xfatal-warnings",
      // Linting options
      "-unchecked",
      "-Xcheckinit",
      "-Xlint:adapted-args",
      "-Xlint:constant",
      "-Xlint:delayedinit-select",
      "-Xlint:deprecation",
      "-Xlint:doc-detached",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-override",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Wdead-code",
      "-Wextra-implicit",
      "-Wnumeric-widen",
      "-Wunused:implicits",
      "-Wunused:imports",
      "-Wunused:locals",
      "-Wunused:params",
      "-Wunused:patvars",
      "-Wunused:privates",
      "-Wvalue-discard",
    ),

    scalacOptions in (Compile, console) ~= filterConsoleScalacOptions,
    scalacOptions in (Test, console)    ~= filterConsoleScalacOptions,

    fork := true,

    libraryDependencies ++=
      Seq(
        "dev.zio"                      %% "zio"                 % "1.0.3",
        "dev.zio"                      %% "zio-interop-cats"    % "2.1.4.0",
        "com.github.pureconfig"        %% "pureconfig"          % "0.12.3",
        "com.softwaremill.sttp.client" %% "core"                % "2.2.3",
        "org.http4s"                   %% "http4s-blaze-server" % "0.21.1",
        "org.http4s"                   %% "http4s-dsl"          % "0.21.0-M2",
        "io.argonaut"                  %% "argonaut"            % "6.3.0",
        "org.apache.kafka"             %  "kafka-clients"       % "2.2.0",
        "ch.qos.logback"               %  "logback-classic"     % "1.2.3",
        "org.scalatest"                %% "scalatest"           % "3.1.1"
      )
  )
