tiny-world
==========

Monadic effect worlds for interacting safely with mutable data.

This is trivial but useful. It does not seem to be a bad idea, but I'm willing to listen to arguments to that effect.

What is this?
-------------

The `State` monad enables pure stateful computations by threading a hunk of state that is available to client code. `tiny-world` provides a similar mechanism, with the twist that the state is available only to a set of primitive actions that are defined by the implementor of a `World`. Client code can use these primitives but has no access to the underlying state, which makes the mechanism safe for use with otherwise unsafe things like mutable values. It's almost exactly the same as `IO`, but the "real world" parameter is instead any value of your choosing, and the primitives can manipulate that value.

Here is a quick example.

```scala
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
    x <- nextInt(5)
    n <- nextPrintableString(x + 5)
    a <- nextInt(100)
  } yield Person(t, n, a)

  // Run that baby
  println(randomPerson.run(3)) // This is pure
  println(randomPerson.run(3)) // This is also pure, so the result will be the same
  println(randomPerson.liftIO.unsafePerformIO) // DANGER, this is impure!

}
```

Kinds of Actions
----------------

Each `World` has its own path-dependent `Action` type that's unique to that world. World implementors have three ways to construct these actions:

* `action(...)` is the most general constructor and is analogous to the familiar `State` action. It takes a function `State => (State, A)` and returns a `State[A]`. This kind of action replaces the world state.
* `effect(...)` take a function `State => A` and produce a `State[A]`. This kind of action does not replace the world state, so it's appropriate for mutable state, or for actions that rely on the state but don't replace it.
* `unit(...)` takes a value `A` and produces a `State[A]`. Use this constructor for actions that do not rely on the world state at all. Note that `unit()` gives you a do-nothing `State[Unit]` which is useful for conditionals.

Kinds of Worlds
---------------

Right now there are two kinds of `World`, although more are likely to emerge. They differ only in the `run` method(s) provided on their actions.

* `PublicWorld` actions consume an initial state, and return the yielded result (`run`) or the final state *and* the yielded result (`eval`). This kind of world is appropriate if you have an existing mutable structure you would like to manipulate in a pure way.
* `PrivateWorld` constructs its own initial state and returns only the final result; `run` takes no arguments and there is no `eval`. Even the type of the state is private to the implementation. This kind of world is useful for completely isolating the interation with a mutable structure and returning only the final result. An example is provided that hides a `java.util.Calendar` in this way, and the end result is an *entirely pure* calendar API because the initial state is always identical and the actions perform no IO. 

Some other kinds of worlds under consideration:

* Explicit `Pure` and `Impure` variants with `run` and `unsafeRun` action methods, respectively. This is just for documentation, but that's probably a good enough reason.
* A `FactoryWorld` would have a fixed initial state, and the `run` method would return the final state (the `eval` method, as with `PublicWorld`, would return both the final state and the yielded result). This would be useful for a `ProcessBuilderWorld` for example.

For more info...
----------------

More here soon. For now see the examples in `src/test`.

You can also find me on #scala and on the Tweeter.
