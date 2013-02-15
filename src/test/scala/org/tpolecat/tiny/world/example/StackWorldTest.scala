package org.tpolecat.tiny.world.example

import scalaz.syntax.monad._
import org.tpolecat.tiny.world.StackWorld

object StackWorldTest extends App {

  class MapWorld[K, V] extends StackWorld {
    type State = collection.mutable.Map[K, V]
    def get(k: K): Action[Option[V]] = effect(_.get(k))
    def put(k: K, v: V): Action[Unit] = effect(_.put(k, v))
    def nop:Action[Unit] = unit()
  }

  object StringIntMapWorld extends MapWorld[String, Int]
  import StringIntMapWorld._

  val action = for {
    a <- get("foo")
    _ <- put("bar", 3)
    _ <- put("foo", 5)
  } yield a

  println(action.eval(collection.mutable.Map("foo" -> 33)))
    
  // Recursive actions can blow the stack. This isn't a problem for worlds
  // based on the free monad.
  val recur:Action[String] = for {
    a <- get("foo")
    _ <- a match {
      case Some(n) if n > 0 => put("foo", n - 1) >> recur
      case _ => nop
    }
  } yield "done"
  
  println(recur.eval(collection.mutable.Map("foo" -> 10))) // ok
  println(recur.eval(collection.mutable.Map("foo" -> 10000))) // blows the stack
  
  
}


