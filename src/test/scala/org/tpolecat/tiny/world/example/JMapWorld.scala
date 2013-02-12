package org.tpolecat.tiny.world.example

import org.tpolecat.tiny.world.PublicWorld

// Use case: some API gives us a Java map and we're supposed to query and 
// mutate it. How can we do this in a pure way?

// First we need to construct an effect world that understands how to manipulate 
// Java maps. We want to pass in an existing map, so the world is public.
trait JMapWorld[K, V] extends PublicWorld {

  // The type of thing we're working with
  type State = java.util.Map[K, V]

  // Primitive actions; typically constructed using effect(...)
  def get(k: K): Action[Option[V]] = effect(_.get(k)).map(Option(_))
  def put(k: K, v: V): Action[Option[V]] = effect(_.put(k, v)).map(Option(_))
  def clear():Action[Unit] = effect(_.clear())

  // A derived action. Note the use of the unit(...) constructor.
  def modify(k: K, f: V => V):Action[Unit] = for {
    v <- get(k)
    _ <- v.map(f).map(put(k, _)).getOrElse(unit())
  } yield ()

}

// What does this look like to the consumer?
object JMapWorldTest extends App {

  // Let's create a Java map with something in it. This is just setup; in real
  // life this is the object that gets handed to us.
  val m = new java.util.HashMap[String, String]
  m.put("foo", "bar")
  println("Initial map state is: " + m) // {foo=bar}

  // Create an effect world that knows about maps. JMapWorld does this out of
  // the box, but we need to provide type arguments. The world is simply a
  // module that exposes a set of actions we can use.
  object MyWorld extends JMapWorld[String, String]
  import MyWorld._

  // Here is some code that updates and queries a map using world actions. This
  // code is pure. MyWorld.Action[A] is a bit like State[java.util.Map[String, 
  // String], A] but the state is entirely hidden; it is impossible for client
  // code to access the underlying map. In a private world even the *type* of
  // the state is hidden.
  val action = for {
    f <- get("foo")
    _ <- clear()
    _ <- put("bar", "qux")
    _ <- modify("bar", _ + "abc")
  } yield "foo was %s".format(f)

  // The impurity is pushed into a single invocation, which we will call out.
  val result = action.run(m) // CAREFUL HERE

  // What have we done?
  println("Result was: " + result)  // foo was Some(bar)
  println("New map state is: " + m) // {bar=quxabc}
  
}