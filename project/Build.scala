import sbt._
import sbt.Keys._
import android.Keys._
import android.Dependencies.apklib
import android.Dependencies.aar

object SoundCamBuild extends Build {
    lazy val app = Project(id = "top", base = file(".")) settings(Seq(
        name := "soundcam",
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.11.7",
        scalacOptions ++= Seq("-optimise", "-Yclosure-elim"),
        scalaSource in Compile <<= baseDirectory (_/"src"),
        libraryDependencies += "com.google.android" % "support-v4" % "r7",
        useProguard in Android := true,
        dexMaxHeap in Android := "2048m"
    ) ++ android.Plugin.androidBuild: _*)

    proguardOptions in Android ++= Seq(
      "-keep public class scala.runtime.BoxedUnit",
      "-keep public class scala.Tuple3")
}
