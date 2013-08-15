package org.tpolecat.tiny.world.example

import scala.util.Random
import org.tpolecat.tiny.world.FreeWorld

/**
 * An example `World` with a small set of `Action`s for random number generation. Although interpreting `Action`s in
 * this `World` involves manipulation of an impure `State`, this impurity is not visible to users; the API is pure.
 */
object RngWorldMinimal extends FreeWorld {

  // Our world's state is an instance of `Random`, but clients have no way to know this, and have no way to get a 
  // reference to the `State` as it is passed through each `Action`.
  protected type State = Random

  // An `Action[A]` is a pure value that (when interpreted) performs a potentially effectful computation on our `State` 
  // and returns a value of type `A`. The `Action` type is path-dependent and unique to this `World`. These `Actions`s
  // are values and their constructors are pure.
  def nextInt = effect(_.nextInt)
  def nextInt(n: Int) = effect(_.nextInt(n))

  // Expose our unit constructor
  def unit[A](a:A) = super.unit(a)
  
  // In order to make our `Action`s runnable, we must provide a public way to invoke `runWorld`. Because the choice of
  // initial `State` and return value are specific to each `World`, this is left to the implementor. Here we provide a 
  // single way to run an `Action` based on a provided seed, which is a pure function.
  implicit class RunnableAction[A](a: Action[A]) {
    def exec(seed: Long): A = runWorld(a, new Random(seed))._2
  }

}

object RngWorldMinimalTest extends App {

  // Import our world's actions
  import RngWorldMinimal._

  // An `Action` that returns a pair of integers, a < 100, b < a
  val pair = for {
    a <- nextInt(100)
    b <- nextInt(a)
  } yield (a, b)

  // Run that baby
  println(pair.exec(0L)) // pure! always returns (60, 28)
  println(pair.exec(0L)) // exactly the same of course
  println(pair.exec(123L)) // (82, 52)

  // Show that our world is trampolined.
  def genMany[A](n: Int, a: Action[A], acc: List[A] = Nil): Action[List[A]] =
    a.flatMap(x => if (n == 0) unit(x :: acc) else genMany(n - 1, a, x :: acc))

  // Many iterations via flatMap
  println(genMany(1000000, nextInt).map(_.sum).exec(0L)) // -340966447

}



