package org.tpolecat.tiny.world.example

import java.security.MessageDigest
import org.tpolecat.tiny.world._
import scalaz.syntax.monad._
import scalaz.syntax.traverse._
import scalaz.std.list._

object MD5World extends PrivateWorld {

  type State = MessageDigest
  def initialState = MessageDigest.getInstance("MD5")

  def digest = effect(_.digest()).map(_.toSeq)
  def digestUTF8(s: String) = digest(s.getBytes("UTF8"))
  def digest(bs: Seq[Byte]) = effect(_.digest(bs.toArray)).map(_.toSeq)
  def digestLength = effect(_.getDigestLength)
  def reset = effect(_.reset())
  def update(bs: Seq[Byte]) = effect(_.update(bs.toArray))
  def updateUTF8(s: String) = update(s.getBytes("UTF8"))
  def nop = unit()

}

object MD5WorldTest extends App {

  import scalaz.syntax.monad._

  import MD5World._

  def md5(ss: List[String]): Action[String] = for {
    _ <- ss.traverse(updateUTF8) // traverse ~ mapM
    n <- digestLength
    d <- digest
  } yield "%d bytes: %s".format(n, d.mkString(" "))

  println(md5(List("foo", "bar", "baz")).run)
  
}
