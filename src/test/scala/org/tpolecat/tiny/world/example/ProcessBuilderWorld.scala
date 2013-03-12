package org.tpolecat.tiny.world.example

import java.lang.ProcessBuilder
import scala.collection.JavaConverters._
import org.tpolecat.tiny.world._
import java.io.File
import scala.io.Source
import scalaz.syntax.monad._

trait ProcessBuilderWorld extends Props { this: EffectWorld =>

  type State = ProcessBuilder

  val directory: Prop[File] = Prop(_.directory, _.directory(_))
  val command: Prop[Seq[String]] = Prop(_.command.asScala.toSeq, _.command(_: _*))
  val redirectErrorStream: Prop[Boolean] = Prop(_.redirectErrorStream, _.redirectErrorStream(_))

  object env {
    def +=(kv: (String, String)): Action[Unit] = effect(_.environment.put(kv._1, kv._2))
    def -=(k: String): Action[Unit] = effect(_.environment.remove(k))
    def apply(s: String): Action[Option[String]] = effect(_.environment.get(s)).map(Option(_))
  }

  implicit class RunnableAction[A](a: Action[A]) {
    def build: ProcessBuilder = runWorld(a, new ProcessBuilder)._1
  }

}

object ProcessBuilderWorldTest extends App with ProcessBuilderWorld with World {

  // Build a process builder to list files in the parent directory, with full details and GMT times
  val proc = for {
    d <- directory
    _ <- directory := new File(d, "..")
    _ <- env += ("TZ" -> "GMT")
    _ <- command := Seq("ls", "-lag")
    _ <- redirectErrorStream := true
  } yield ()

  // This is a pure operation, although equality on ProcessBuilder is bogus
  val pb: ProcessBuilder = proc.build

  // These are not the droids you're looking for
  Source.fromInputStream(pb.start.getInputStream).getLines.foreach(println)

}



