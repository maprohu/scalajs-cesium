import java.util.jar.Attributes

val githubRepo = "scalajs-cesium"
val cesiumVersion = "1.16"
val osgiVersion = "5.0.0"

val commonSettings = Seq(
  organization := "com.github.maprohu",
  version := s"$cesiumVersion.0-SNAPSHOT",
  resolvers += Resolver.sonatypeRepo("snapshots"),

  scalaVersion := "2.11.7",
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
//      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      Some(sbtglobal.SbtGlobals.prodRepo)
  },
  pomIncludeRepository := { _ => false },
  licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php")),
  homepage := Some(url(s"https://github.com/maprohu/${githubRepo}")),
  pomExtra := (
    <scm>
      <url>git@github.com:maprohu/{githubRepo}.git</url>
      <connection>scm:git:git@github.com:maprohu/{githubRepo}.git</connection>
    </scm>
    <developers>
      <developer>
        <id>maprohu</id>
        <name>maprohu</name>
        <url>https://github.com/maprohu</url>
      </developer>
    </developers>
  ),

  crossPaths := false
)

val noPublish = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

lazy val jsdocgenLib = ProjectRef(uri("../scalajs-jsdocgen"), "lib")
lazy val facade = project
  .settings(commonSettings)
  .dependsOn(jsdocgenLib)
  .enablePlugins(JsdocPlugin, ScalaJSPlugin)
  .settings(
    publishArtifact in (Compile, packageDoc) := false,
    name := "scalajs-cesium",
    jsdocRunSource := Some(
      uri(s"https://github.com/AnalyticalGraphicsInc/cesium.git#${cesiumVersion}")
    ),
    jsdocRunInputs := Seq("Source"),
//    jsdocRunTarget := (resourceDirectory in Compile).value / "cesium-jsdoc.json",

    jsdocTarget := (sourceManaged in Compile).value,
// comment to do jsdoc run
    jsdocDocletsFile := baseDirectory.value / "work" / "cesium-jsdoc.json",
    jsdocGlobalScope := Seq("cesium"),
    jsdocUtilScope := "pkg",

    sourceGenerators in Compile += jsdocGenerate.taskValue,
    jsdocSourceFileRoot := uri("file:/home/maprohu/git/scalajs-cesium/facade/target/jsdocgenwork/c6269b0263a4f834e250/cesium"),
    jsdocSourcePublishRoot := uri(s"https://github.com/AnalyticalGraphicsInc/cesium/blob/$cesiumVersion/"),
//    jsDependencies ++= Seq(
//      "org.webjars.bower" % "cesium" % cesiumVersion / s"webjars/cesiumjs/${cesiumVersion}.0/CesiumUnminified/Cesium.js" minified s"webjars/cesiumjs/${cesiumVersion}.0/CesiumUnminified/Cesium.js"
//    ),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0"
    ),
    mappings in (Compile, packageSrc) ++=
      (managedSources in Compile).value pair relativeTo((sourceManaged in Compile).value)

  )

lazy val testapp = project
  .settings(commonSettings)
  .settings(noPublish)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(facade)
  .settings(
    persistLauncher in Compile := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0"
    )

  )

lazy val assets = project
  .enablePlugins(SbtOsgi)
  .settings(
    OsgiKeys.additionalHeaders ++= Map(
      Attributes.Name.IMPLEMENTATION_VERSION.toString -> version.value
    ),
    organization := "com.github.maprohu",
    name := "cesiumjs-assets",
    version := s"$cesiumVersion.0",
    publishArtifact in packageDoc := false,
    OsgiKeys.exportPackage := Seq("com.github.maprohu.cesium"),
    libraryDependencies ++= Seq(
      "org.osgi" % "org.osgi.core" % osgiVersion % Provided
    ),
    crossPaths := false,
    resourceGenerators in Compile += Def.task {
      val f = (resourceManaged in Compile).value / "com" / "github" / "maprohu" / "cesium" / "assets.zip"
      IO.download(url(s"http://cesiumjs.org/releases/Cesium-$cesiumVersion.zip"), f)
      IO.unzip(f, target.value / "dist")
      Seq(f)
    }.taskValue,
    publishTo := Some(sbtglobal.SbtGlobals.prodRepo)


  )

lazy val root = (project in file("."))
  .settings(noPublish)
  .aggregate(facade, testapp)