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

Action Laws
-----------

* The monadic composition of deterministic `Action`s is also deterministic.
* The execution of a deterministic action is pure.



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
