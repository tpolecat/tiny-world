package org.tpolecat.tiny.world.example

import language.higherKinds
import scala.collection.generic.CanBuildFrom
import scala.util.Random
import scalaz.effect.IO

import org.tpolecat.tiny.world._

/**
 * A `World` with a complete set of primitives for random number generation. This `World` wraps a `scala.util.Random`
 * but that fact is entirely hidden from users, who have no way to even know what the `State` type is (much less get a
 * reference to it). Users of `RngWorld` can manipulate this state through monadic `Action`s (which are pure) and can
 * execute these actions in a pure or impure context.
 */
object RngWorld extends World {

  // Our world's state is an instance of `Random`
  protected type State = Random

  // An `Action[A]` is a pure value that (when interpreted) performs a potentially effectful computation on our `State` 
  // and returns a value of type `A`. The `Action` type is path-dependent and unique to this `World`.
  def nextBoolean: Action[Boolean] = effect(_.nextBoolean)

  // Other trivial actions (return types omitted)
  def nextDouble = effect(_.nextDouble)
  def nextFloat = effect(_.nextFloat)
  def nextGaussian = effect(_.nextGaussian)
  def nextInt = effect(_.nextInt)
  def nextInt(n: Int) = effect(_.nextInt(n))
  def nextLong = effect(_.nextLong)
  def nextPrintableChar = effect(_.nextPrintableChar)
  def nextPrintableString(len: Int) = effect(_.alphanumeric.take(len).mkString)
  def nextString(len: Int) = effect(_.nextString(len))
  def setSeed(seed: Long) = effect(_.setSeed(seed))

  // A derived Action for choice. `Action` is monadic, so we can use `map` for great good. 
  def choose[A](as: A*): Action[A] = nextInt(as.length).map(as)

  // Shuffle is trivial but has a horrid type signature (taken verbatim from the `shuffle` method we're delegating to).
  def shuffle[T, F[X] <: TraversableOnce[X]](xs: F[T])(implicit bf: CanBuildFrom[F[T], T, F[T]]): Action[F[T]] =
    effect(_.shuffle(xs))

  // For bytes we want to return an immutable structure, so we have a temporary `Array[Byte]` that we immediately turn
  // into a `List[Byte]`.
  def nextBytes(len: Int): Action[List[Byte]] = effect { r =>
    val bs = new Array[Byte](len)
    r.nextBytes(bs)
    bs.toList
  }

  // In order to make our `Action`s runnable, we must provide a public way to invoke `runWorld`. Because the choice of
  // initial `State` and return value are specific to each `World`, this is left to the user. Here we provide two ways
  // of running an `Action`. The first consumes a seed value and is referentially transparent. The second returns an IO
  // action that uses the system clock for the random seed.
  implicit class RunnableAction[A](a: Action[A]) {
    def run(seed: Long): A = runWorld(a, new Random(seed))._2
    def liftIO: IO[A] = IO(System.currentTimeMillis).map(run)
  }

}

object RngWorldTest extends App {

  // Import the `Action` constructors from `RngWorld`
  import RngWorld._

  // A simple data type
  case class Person(title: String, name: String, age: Int)

  // An action to generate a random Person.
  val randomPerson = for {
    t <- choose("Mr", "Mrs", "Dr")
    x <- nextInt(5).map(_ + 5)
    n <- nextPrintableString(x)
    a <- nextInt(100)
  } yield Person(t, n, a)

  // Run that baby
  println(randomPerson.run(3)) // This is pure
  println(randomPerson.run(3)) // This is also pure, so the result will be the same
  println(randomPerson.liftIO.unsafePerformIO) // DANGER, this is impure!

}



