package org.tpolecat.tiny.world.example.slick

import scalaz.std.list
import scala.slick.driver.H2Driver.simple._
import scalaz.effect.IO
import scalaz.effect.IO.putStrLn
import scalaz.effect.SafeApp

// A pure functional database program.
object FirstExampleMinimal extends SafeApp {

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

  // Our actions are values that compose monadically to create larger actions.
  // Note that we never mention the session

  val ddl: Action[Unit] =
    Coffees.ddl.lift.create

  val insertRows: Action[Option[Int]] =
    Coffees.lift.insertAll(
      ("Peet's", 7.99, 0, 0),
      ("Starbucks", 8.99, 0, 0),
      ("Stumptown", 9.99, 0, 0))

  val query: Action[Unit] =
    Query(Coffees).lift.foreach {
      case (name, price, sales, total) =>
        putStrLn("  " + name + "\t" + price + "\t" + sales + "\t" + total).lift
    }

  // Put them together. The actions are just values so they could be done inline,
  // as shown in FirstExampleMinimal2
  val action: Action[Unit] =
    for {
      _ <- ddl
      _ <- insertRows
      _ <- putStrLn("Coffees:").lift
      _ <- query
    } yield ()

  // Our database
  val db = Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver")

  // Turn an action into a transaction by applying it to a database.
  val tx = action(db)

  // Our IO[Unit] that runs at the end of the world
  override val runc: IO[Unit] =
    for {
      _ <- putStrLn("About to run transaction:")
      _ <- tx 
      _ <- putStrLn("Done.")
    } yield ()

}
