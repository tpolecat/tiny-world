tiny-world
==========

Monadic effect worlds for interacting safely with mutable data.

This is trivial but useful. It does not seem to be a bad idea, but I'm willing to listen to arguments to that effect.

What is this?
-------------

The `State` monad enables pure stateful computations by threading a hunk of state that is available to client code. `tiny-world` provides a similar mechanism, with the twist that the state is available only to a set of primitive actions that are defined by the implementor of a `World`. Client code can use these primitives but has no access to the underlying state, which makes the mechanism safe for use with otherwise unsafe things like mutable values. It's almost exactly the same as `IO`, but the "real world" parameter is instead any value of your choosing, and the primitives can manipulate that value.

Here is a minimal example.

```scala
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
```

Kinds of Actions
----------------

Each `World` has its own path-dependent `Action` type that's unique to that world. World implementors have three ways to construct these actions:

* `action(...)` is the most general constructor and is analogous to the familiar `State` action. It takes a function `State => (State, A)` and returns a `State[A]`. This kind of action replaces the world state.
* `effect(...)` take a function `State => A` and produce a `State[A]`. This kind of action does not replace the world state, so it's appropriate for mutable state, or for actions that rely on the state but don't replace it.
* `unit(...)` takes a value `A` and produces a `State[A]`. Use this constructor for actions that do not rely on the world state at all. Note that `unit()` gives you a do-nothing `State[Unit]` which is useful for conditionals.

Examples
--------

There are a bunch of examples in `src/test` that apply this technique to some common irritating APIs (including `java.util.Calendar`, which seems like it could evolve into something genuinely useful). 

For more info...
----------------

Comments and suggestions are welcome. 

You can also me on `#scala` and on the Tweeter.


