organization := "daily.practice"

name := "tyler"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

seq(webSettings :_*)

libraryDependencies ++= Seq(
	"org.scalatra"              %% "scalatra"               % "2.0.4",
	"org.scalatra"              %% "scalatra-scalate"       % "2.0.4",
	"org.scalatra"              %% "scalatra-specs2"        % "2.0.4"           % "test",
	"org.specs2"                %% "specs2"                 % "1.8.2"           % "test",
	"ch.qos.logback"            % "logback-classic"         % "1.0.0"           % "runtime",
	"org.eclipse.jetty"         % "jetty-webapp"            % "7.5.4.v20111024" % "container",
	"javax.servlet"             % "servlet-api"             % "2.5"             % "provided",
	"net.liftweb"               %% "lift-json"              % "2.4",
	"com.twitter"               % "util-logging"            % "1.12.4",
	"com.sun.jersey"            % "jersey-core"             % "1.12",
	"com.sun.jersey"            % "jersey-client"           % "1.12"
)

resolvers += "twitter.com" at "http://maven.twttr.com/"
