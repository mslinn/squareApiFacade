organization := "com.micronautics"
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
name := "square_api"
version := "0.1.1"
scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.10.6", "2.11.8")

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.7", "-unchecked",
    "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")

scalacOptions in (Compile, doc) <++= baseDirectory.map {
  (bd: File) => Seq[String](
     "-sourcepath", bd.getAbsolutePath,
     "-doc-source-url", "https://github.com/mslinn/changeMe/tree/master€{FILE_PATH}.scala"
  )
}

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-g:vars")

resolvers ++= Seq(
  "Lightbend Releases" at "http://repo.typesafe.com/typesafe/releases",
  "micronautics/scala on bintray" at "http://dl.bintray.com/micronautics/scala"
)

libraryDependencies ++= Seq(
  "com.mashape.unirest"     %  "unirest-java"       % "1.4.9" withSources(),
  "com.github.nscala-time"  %% "nscala-time"        % "2.12.0" withSources()
)

logLevel := Level.Warn

// Only show warnings and errors on the screen for compilations.
// This applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn

// Level.INFO is needed to see detailed output when running tests
logLevel in test := Level.Info

// define the statements initially evaluated when entering 'console', 'console-quick', but not 'console-project'
initialCommands in console := """
                                |""".stripMargin

cancelable := true

bintrayOrganization := Some("micronautics")
bintrayRepository := "scala"
