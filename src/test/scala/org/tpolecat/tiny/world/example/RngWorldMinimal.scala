package org.tpolecat.tiny.world.example

import scala.util.Random
import org.tpolecat.tiny.world._

/**
 * An example `World` with a small set of `Action`s for random number generation. Although interpreting `Action`s in
 * this `World` involves manipulation of an impure `State`, this impurity is not visible to users; the API is pure.
 */
object RngWorldMinimal extends World {

  // Our world's state is an instance of `Random`, but clients have no way to know this, and have no way to get a 
  // reference to the `State` as it is passed through each `Action`.
  protected type State = Random

  // An `Action[A]` is a pure value that (when interpreted) performs a potentially effectful computation on our `State` 
  // and returns a value of type `A`. The `Action` type is path-dependent and unique to this `World`. These `Actions`s
  // are values and their constructors are pure.
  def nextInt = effect(_.nextInt)
  def nextInt(n: Int) = effect(_.nextInt(n))

  // In order to make our `Action`s runnable, we must provide a public way to invoke `runWorld`. Because the choice of
  // initial `State` and return value are specific to each `World`, this is left to the implementor. Here we provide a 
  // single way to run an `Action` based on a provided seed, which is a pure function.
  implicit class RunnableAction[A](a: Action[A]) {
    def run(seed: Long): A = runWorld(a, new Random(seed))._2    
  }

}

object RngWorldMinimalTest extends App {

  // Import our `Action`s 
  import RngWorldMinimal._

  // An `Action` that returns a pair of integers, a < 100, b < a
  val pair = for {
    a <- nextInt(100)
    b <- nextInt(a)
  } yield (a, b)
  
  // Run that baby
  println(pair.run(0L)) // pure! always returns (60, 28)
  println(pair.run(0L)) // exactly the same of course
  println(pair.run(123L)) // (82, 52)

}



