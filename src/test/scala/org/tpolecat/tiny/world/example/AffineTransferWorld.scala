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
  // primitives, and will not retain or share the state with anyone else.
  val (state, result) = action.eval(new AffineTransform) // CAREFUL HERE

  // What have we done?
  println(result) // "Determinant was -2, then -1"
  println(state) // AffineTransform[[0.0, -1.0, 0.0], [-0.5, -0.0, 0.0]]

}

