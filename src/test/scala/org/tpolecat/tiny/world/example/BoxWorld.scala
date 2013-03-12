package org.tpolecat.tiny.world.example

import scalaz.syntax.monad._
import scalaz.syntax.id._
import org.tpolecat.tiny.world.World

// The simplest possible mutable object
class Box(var n: Int) {
  def mod(f: Int => Int) = { n = f(n) }
  override def toString = "Box(%d)".format(n)
}

// A world for manipulating a Box
object BoxWorld extends World {

  // Our state type
  type State = Box

  // Primitive operations
  def get: Action[Int] = effect(b => b.n)
  def mod(f: Int => Int): Action[Int] = effect(b => b mod f) >> get
  def nop: Action[Unit] = unit()

  implicit class RunnableAction[A](a: Action[A]) {
    def eval(b: Box) = runWorld(a, b)
  }

}

// A test for our box
object BoxWorldTest extends App {

  import BoxWorld._

  // Manipulate the state
  def foo(n: Int) = for {
    a <- get
    b <- mod(_ + n)
    c <- mod(_ * n)
  } yield (a, b, c)

  // Applicative style!
  def bar(n: Int) = (get |@| mod(_ + n) |@| mod(_ * n)).tupled

  println(foo(3).eval(new Box(2))) // (Box(15),(2,5,15))
  println(bar(3).eval(new Box(2))) // (Box(15),(2,5,15))

  // Recursive action will never blow the stack
  val countDown: Action[String] = for {
    a <- get
    _ <- if (a == 0) nop else mod(_ - 1) >> countDown
  } yield "done"

  println(countDown.eval(new Box(10000))) // (Box(0),done)

}


