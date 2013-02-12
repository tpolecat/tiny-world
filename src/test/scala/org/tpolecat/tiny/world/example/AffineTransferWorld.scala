package org.tpolecat.tiny.world.example

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

