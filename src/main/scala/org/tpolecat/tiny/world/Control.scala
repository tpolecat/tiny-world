package org.tpolecat.tiny.world

import language.higherKinds
import scalaz.Monad
import scalaz.syntax.monad._

// TODO: this probably exists in scalaz 
trait Control { 

  def whileM[M[_]:Monad](test: M[Boolean], a: => M[_]): M[Unit] =
    ifM(test, a >> whileM(test, a))

  def doUntilM[M[_]:Monad](a: => M[_], test: M[Boolean]): M[Unit] =
    a >> ifM(test.map(!_), doUntilM(a, test))

  def ifM[M[_]:Monad] (test: M[Boolean], a: => M[_]): M[Unit] =
    test.flatMap(if (_) a.map(_ => ()) else ().point)
    
}
