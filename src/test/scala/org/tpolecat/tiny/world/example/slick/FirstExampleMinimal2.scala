package org.tpolecat.tiny.world.example.slick

import scalaz.std.list
import scala.slick.driver.H2Driver.simple._
import scalaz.effect.IO
import scalaz.effect.IO.putStrLn
import scalaz.effect.SafeApp

// A pure functional database program.
object FirstExampleMinimal2 extends SafeApp {

  // Construct an effect world for executing Slick actions with an H2Driver.
  val world = SlickWorld(scala.slick.driver.H2Driver)
  import world._

  // Definition of the COFFEES table (standard Slick)
  object Coffees extends Table[(String, Double, Int, Int)]("COFFEES") {
    def name = column[String]("COF_NAME", O.PrimaryKey)
    def price = column[Double]("PRICE")
    def sales = column[Int]("SALES")
    def total = column[Int]("TOTAL")
    def * = name ~ price ~ sales ~ total
  }  
  
  // Our action is a *value* of type Action[Unit]; i.e., Database => IO[Unit]
  val action: Action[Unit] = for {

    // DDL execution is the same, but must be lifted
    _ <- Coffees.ddl.lift.create

    // Same with table operations
    _ <- Coffees.lift.insertAll(
      ("Peet's", 7.99, 0, 0),
      ("Starbucks", 8.99, 0, 0),
      ("Stumptown", 9.99, 0, 0))
      
    // IO Actions must also be lifted
    _ <- putStrLn("Coffees:").lift
    
    // Queries too
    _ <- Query(Coffees).lift.foreach {
      case (name, price, sales, total) =>
        putStrLn("  " + name + "\t" + price + "\t" + sales + "\t" + total).lift
    }

  } yield () // our final answer, which is nothing

  // Our database
  val db = Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver")
  
  // Our IO[Unit] that runs at the end of the world
  override val runc: IO[Unit] = 
    action(db)

}
