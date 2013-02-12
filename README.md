tiny-world
==========

Monadic effect worlds for interacting safely with mutable data.

This is trivial but useful. It does not seem to be a bad idea, but I'm willing to listen to arguments to that effect.

What is this?
-------------

The `State` monad enables pure stateful computations by threading a hunk of state that is available to client code. `tiny-world` provides a similar mechanism, with the twist that the state is available only to a set of primitive actions that are defined by the implementor of a `World`. Client code can use these primitives but has no access to the underlying state, which makes the mechanism safe for use with otherwise unsafe things like mutable values. It's somewhat like `IO` but the "real world" parameter is instead any value of your choosing, and the primitives can manipulate that value.

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

For more info...
----------------

More here soon. For now see the examples in `src/test`.

You can also find me on #scala and on the Tweeter.