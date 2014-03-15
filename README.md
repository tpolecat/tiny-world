tiny-world
==========

You know? That thing? Where you wrap an unsafe Java doodad and make a pure monadic API? Tiny-world abstracts this pattern, and does it in such a way that users of your API have no knowledge of the passed state (not even its type) unless you say so.

There are three implementations, but probably the one you want to use is `FreeWorld`, which is also the simplest. Honestly just copy and paste it; this is more of a snippet than a library. Anyway you create a module with your primitive operations that extends a world, and users just import splat from that module. Here's an example.

```scala
import scalaz.effect.IO
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
  def unit[A](a: A) = super.unit(a)

  // In order to make our `Action`s runnable, we must provide a public way to invoke `runWorld`. Because the choice of
  // initial `State` and return value are specific to each `World`, this is left to the user. Here we provide two ways
  // of running an `Action`. The first consumes a seed value and is referentially transparent. The second returns an IO
  // action that uses the system clock for the random seed.
  implicit class RunnableAction[A](a: Action[A]) {
    def exec(seed: Long): A = runWorld(a, new Random(seed))._2
    def liftIO: IO[A] = IO(exec(System.currentTimeMillis))
  }

}

object RngWorldMinimalTest extends App {

  // Import our world's actions
  import RngWorldMinimal.{ Action => Rng, _ }

  // An `Action` that returns a pair of integers, a < 100, b < a
  val pair: Rng[(Int, Int)] =
    for {
      a <- nextInt(100)
      b <- nextInt(a)
    } yield (a, b)

  // Run that baby
  println(pair.exec(0L))   // pure! always returns (60, 28)
  println(pair.exec(0L))   // exactly the same of course
  println(pair.exec(123L)) // (82, 52)
  println(pair.liftIO.unsafePerformIO) // DANGER: impure, who knows what will happen?

  // Show that our world is trampolined.
  def genMany[A](n: Int, a: Rng[A], acc: List[A] = Nil): Rng[List[A]] =
    a.flatMap(x => if (n == 0) unit(x :: acc) else genMany(n - 1, a, x :: acc))

  // Many iterations via flatMap
  println(genMany(1000000, nextInt).map(_.sum).exec(0L)) // -340966447

}
```

For more info...
----------------

Tons of examples down in `src/test`.

You can find me on `#scala` and on the Tweeter.


