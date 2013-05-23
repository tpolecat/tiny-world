package org.tpolecat.tiny.world.example

import org.tpolecat.tiny.world.World
import scala.reflect.ClassTag
import scalaz._
import Scalaz._
import scala.collection.generic.CanBuildFrom
import language.higherKinds

class ArrayWorld[A: ClassTag] extends World {

  protected type State = Array[A]

  def length: Action[Int] = effect(_.length)

  def realloc(n: Int, a: => A): Action[Unit] =
    action { s =>
      val s0 = Array.fill(n)(a)
      Array.copy(s, 0, s, 0, n)
      (s0, ())
    }

  def put(n: Int, a: A): Action[Boolean] =
    effect { s =>
      if (n <= s.length) {
        s(n) = a
        true
      } else false
    }

  def get(n: Int): Action[Option[A]] =
    effect(_.lift(n))

  implicit class FoldableAction[A](a: Action[Option[A]]) {
    def fold[B](b: B)(f: A => B) = a.map(_.fold(b)(f))
  }

  def getFold[B](n: Int)(a: B)(f: A => B): Action[B] =
    get(n).fold(a)(f)

  implicit class RunnableAction[B: ClassTag](a: Action[B]) {
    def run[C[A] <: Traversable[A]](as: C[A])(implicit ev: CanBuildFrom[C[A], A, C[A]]): (C[A], B) = 
      runWorld(a, as.toArray).bimap(_.to[C], identity)
  }

}

object ArrayWorldTest extends App {
  
  val w = new ArrayWorld[Int]
  import w._
  
  val action:Action[String] = 
    for {
      a <- get(0).fold("nothing")("found " + _)
      _ <- put(1, 99)
    } yield a
  
  val (a, b) = action.run(List(1,2,3))
  
  println(a, b) // (List(1, 99, 3),found 1)
  
}

