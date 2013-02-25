package org.tpolecat.tiny.world.example

import java.lang.ProcessBuilder
import scala.collection.JavaConverters._
import org.tpolecat.tiny.world._
import java.io.File

object ProcessBuilderWorld extends ImpureFactoryWorld {

  type State = ProcessBuilder
  protected def initialState = new ProcessBuilder

  def command: Action[List[String]] = effect(_.command.asScala.toList)
  def command(cmd: String*): Action[Unit] = effect(_.command(cmd: _*))

  def directory: GetterSetter[File] = getterSetter(_.directory, _.directory(_))

  object env {
    def +=(kv: (String, String)): Action[Unit] = effect(_.environment.put(kv._1, kv._2))
    def -=(k: String): Action[Unit] = effect(_.environment.remove(k))
    def apply(s: String): Action[Option[String]] = effect(_.environment.get(s)).map(Option(_))
  }

  def redirectErrorStream: Action[Boolean] = effect(_.redirectErrorStream)
  def redirectErrorStream(b: Boolean): Action[Unit] = effect(_.redirectErrorStream(b))

}

object ProcessBuilderWorldTest extends App {

  import ProcessBuilderWorld._

  val proc = for {
    d <- directory
    _ <- directory := new File(d, "foo")
    _ <- env += ("foo" -> "bar")
    _ <- command("ls")
    _ <- redirectErrorStream(true)
  } yield ()

  // This operation is impure because it returns a new value each time
  val pb: ProcessBuilder = proc.unsafeBuild

  println(pb.directory.getAbsolutePath)
  
}



