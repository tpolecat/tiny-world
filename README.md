tiny-world
==========

Monadic effect worlds for great good! This is a bit like `State` and `StateT` with existential state that's private by default. This allows for constructing effect worlds that can't leak state or any knowledge thereof (including the type). This is equivalent to (and simpler than) many applications of `Free`.

`World` is implemented on top of `Free` (via `Trampoline`) but `WorldT` is not (yet). So keep that in mind in the unlikely event that you play around with this stuff for real.

What's new?
------------

I just added `ActionT` and a `MonadIO` instance for it, so you can now stack like a boss. There's a new example called `IOWorld` that demonstrates this. It's not quite baked but it does seem to work. *UPDATE* just added an example turning a simple Slick program into pure functional awesomeness.

I also added an STM example that hides the transactional machinery. Users don't even know they're constructing transactions!

What is this?
-------------

The `State` monad enables pure stateful computations by threading a hunk of state that is available to client code. `tiny-world` provides a similar mechanism, with the twist that the state is available only to a set of primitive actions that are defined by the implementor of a `World`. Client code can use these primitives but has no access to the underlying state, which makes the mechanism safe for use with otherwise unsafe things like mutable values. It's almost exactly the same as `IO`, but the "real world" parameter is instead any value of your choosing, and the primitives can manipulate that value.

Here is a minimal example.

```scala
package org.tpolecat.tiny.world.example

import scala.util.Random
import org.tpolecat.tiny.world.FreeWorld

/**
 * An example `World` with a small set of `Action`s for random number generation. Although
 * interpreting `Action`s in this `World` involves manipulation of an impure `State`, this impurity
 * is not visible to users; the API is pure.
 */
object RngWorldMinimal extends FreeWorld {

  // Our world's state is an instance of `Random`, but clients have no way to know this, and have 
  // no way to get a reference to the `State` as it is passed through each `Action`.
  protected type State = Random

  // An `Action[A]` is a pure value that (when interpreted) performs a potentially effectful 
  // computation on our `State` and returns a value of type `A`. The `Action` type is path-dependent 
  // and unique to this `World`. These `Actions`s are values and their constructors are pure.
  def nextInt = effect(_.nextInt)
  def nextInt(n: Int) = effect(_.nextInt(n))

  // Expose our unit constructor
  def unit[A](a: A) = super.unit(a)

  // In order to make our `Action`s runnable, we must provide a public way to invoke `runWorld`. 
  // Because the choice of initial `State` and return value are specific to each `World`, this is 
  // left to the implementor. Here we provide a single way to run an `Action` based on a provided
  // seed, which is a pure function.
  implicit class RunnableAction[A](a: Action[A]) {
    def exec(seed: Long): A = runWorld(a, new Random(seed))._2
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

  // Show that our world is trampolined.
  def genMany[A](n: Int, a: Rng[A], acc: List[A] = Nil): Rng[List[A]] =
    a.flatMap(x => if (n == 0) unit(x :: acc) else genMany(n - 1, a, x :: acc))

  // Many iterations via flatMap
  println(genMany(1000000, nextInt).map(_.sum).exec(0L)) // -340966447

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

You can find me on `#scala` and on the Tweeter.


