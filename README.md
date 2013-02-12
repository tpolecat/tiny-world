tiny-world
==========

Monadic effect worlds for interacting safely with mutable data.

This is trivial but useful. 

What is this?
-------------

The `State` monad enables pure stateful computations by threading a hunk of state that is available to client code. `tiny-world` provides a similar mechanism, with the twist that the state is available only to a set of primitive actions that are defined by the implementor of a `World`. Client code can use these primitives but has no access to the underlying state, which makes the mechanism safe for use with otherwise unsafe things like mutable values. It's somewhat like `IO` but the "real world" parameter is instead any value of your choosing, and the primitives can manipulate that value.

Here is a quick example.

```scala
import org.tpolecat.tiny.world.PublicWorld
import java.awt.geom.AffineTransform

// An effect world that understands AWT affine transform objects
object AffineTransferWorld extends PublicWorld {

  type State = AffineTransform

  // Some actions
  def determinant: Action[Double] = effect(_.getDeterminant)
  def invert:Action[Unit] = effect(_.invert())
  def rotate(theta:Double):Action[Unit] = effect(_.rotate(theta))
  def scale(sx:Double, sy:Double):Action[Unit] = effect(_.scale(sx, sy))
  
}

object AffineTransferWorldTest extends App {
  
  import AffineTransferWorld._
  
  // A pure action that produces a string.
  val action:Action[String] = for {
    _ <- scale(2.0, -1.0)
    a <- determinant
    _ <- rotate(math.Pi / 2)
    _ <- invert
    b <- determinant
  } yield "Determinant was %2.0f, then %2.0f".format(a, b)
  
  // Running the action is impure, but we can isolate the impurity and call
  // it out clearly.
  val (state, result) = action.eval(new AffineTransform) // CAREFUL HERE
  
  // What have we done?
  println(result) // "Determinant was -2, then -1"
  println(state)  // AffineTransform[[0.0, -1.0, 0.0], [-0.5, -0.0, 0.0]]

}
```

For more info...
----------------

More here soon. For now see the examples in `src/test`.

You can also find me on #scala and on the Tweeter.