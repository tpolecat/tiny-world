tiny-world
==========

Monadic effect worlds for interacting safely with mutable data.

This is trivial but useful. It does not seem to be a bad idea, but I'm willing to listen to arguments to that effect.

What is this?
-------------

The `State` monad enables pure stateful computations by threading a hunk of state that is available to client code. `tiny-world` provides a similar mechanism, with the twist that the state is available only to a set of primitive actions that are defined by the implementor of a `World`. Client code can use these primitives but has no access to the underlying state, which makes the mechanism safe for use with otherwise unsafe things like mutable values. It's almost exactly the same as `IO`, but the "real world" parameter is instead any value of your choosing, and the primitives can manipulate that value.

Here is a quick example.

```scala
package org.tpolecat.tiny.world.example

import org.tpolecat.tiny.world.PublicWorld
import java.awt.geom.AffineTransform

// An effect world that understands AWT affine transform objects
object AffineTransferWorld extends PublicWorld {

  type State = AffineTransform

  // Some actions (a real implementation would have many more)
  def determinant: Action[Double] = effect(_.getDeterminant)
  def invert: Action[Unit] = effect(_.invert())
  def rotate(theta: Double): Action[Unit] = effect(_.rotate(theta))
  def scale(sx: Double, sy: Double): Action[Unit] = effect(_.scale(sx, sy))

}

// Client code uses the world thus:
object AffineTransferWorldTest extends App {

  // The world is just a module that exposes actions for our use
  import AffineTransferWorld._

  // A pure action that produces a String. The action might manipulate an
  // underlying AffineTransform when it is "run" at a later time. The action
  // has no access to the state itself, so references cannot leak.
  val action: Action[String] = for {
    _ <- scale(2.0, -1.0)
    a <- determinant
    _ <- rotate(math.Pi / 2)
    _ <- invert
    b <- determinant
  } yield "Determinant was %2.0f, then %2.0f".format(a, b)

  // Running the action is impure, but we can isolate the impurity and call
  // it out clearly. We can be confident that the action, no matter what it
  // does, will not manipulate the state in any way disallowed by our 
  // primitives, nor will it retain or share the state with anyone else.
  val (state, result) = action.eval(new AffineTransform) // CAREFUL HERE

  // What have we done?
  println(result) // "Determinant was -2, then -1"
  println(state) // AffineTransform[[0.0, -1.0, 0.0], [-0.5, -0.0, 0.0]]

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
