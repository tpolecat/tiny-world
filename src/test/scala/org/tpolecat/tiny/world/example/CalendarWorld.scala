package org.tpolecat.tiny.world.example

import java.util.Calendar
import org.tpolecat.tiny.world._
import scalaz.syntax.monad._

object CalendarWorld extends PrivateWorld with Control {
  
  protected type State = Calendar
  protected def initialState = { val c = Calendar.getInstance; c.clear(); c }

  // Private actions
  private def add(field: Int): Action[Unit] = effect(_.clear(field))
  private def get(field: Int): Action[Int] = effect(_.get(field))
  private def add(field: Int, amount: Int): Action[Unit] = effect(_.add(field, amount))
  // etc
  
  def clear(): Action[Unit] = effect(_.clear())
  def setTimeInMillis(millis: Long): Action[Unit] = effect(_.setTimeInMillis(millis))
  def year = get(Calendar.YEAR)
  def month = get(Calendar.MONTH).map(_ + 1)
  def day = get(Calendar.DAY_OF_MONTH)
  def addDay(n:Int) = add(Calendar.DAY_OF_MONTH, n)
  def addMonth(n:Int) = add(Calendar.MONTH, n)
  // etc
  
}

object CalTest extends App {

  import CalendarWorld._
  
  val ymd = for {
    _ <- whileM(day.map(_ != 25), addDay(1))
    _ <- doUntilM(addMonth(1), month.map(_ == 10))
    y <- year
    m <- month
    d <- day
  } yield (y, m, d)

  println(ymd.run)

  val ymd2 = (year |@| month |@| day).tupled

  println(ymd2.run)

}

