package org.tpolecat.tiny.world.example.slick

import language.higherKinds
import scala.slick.driver.BasicDriver
import scala.slick.lifted.DDL
import scala.slick.session.Database
import scala.slick.session.Session
import org.tpolecat.tiny.world.TWorld
import scalaz.effect.IO
import scala.slick.driver.ExtendedProfile
import scalaz._
import Scalaz._
import scala.slick.jdbc.UnitInvokerMixin
import scala.slick.jdbc.StatementInvoker
import scala.slick.jdbc.UnitInvoker
import scala.slick.util.CloseableIterator

object SlickWorldT extends TWorld {
  protected type State = Session
}

case class SlickWorld[E <: ExtendedProfile](driver: E) extends SlickWorldT.Lifted[IO] {
  import driver.simple._

  implicit class Query0[A, B](val q: Query[A, B]) {
    def lift = LiftedQuery(q)
  }

  case class LiftedQuery[A, B](q: Query[A, B]) {

    def list: Action[List[B]] = effect(q.list()(_))
    def first: Action[B] = effect(q.first()(_))
    def firstOption: Action[Option[B]] = effect(q.firstOption()(_))

    def foreach(f: B => Action[Unit]): Action[Unit] = {
     
      // N.B. this will blow the stack because WorldT is not implemented
      // in terms of Free. So, need to fix this.
      def iterate(it: CloseableIterator[B]): Action[Unit] =
        for {
          n <- IO(it.hasNext).lift
          _ <- if (n) {
            for {
              a <- IO(it.next).lift
              _ <- f(a)
              _ <- iterate(it)
            } yield ()
          } else unit()
        } yield ()

      for {
        i <- action(s => (s, q.elements()(s)))
        _ <- iterate(i)
      } yield ()
      
    }

  }

  // DDL
  implicit class DDL0[A, B](val ddl: DDL) {
    def lift = LiftedDDL(ddl)
  }

  case class LiftedDDL(ddl: DDL) {
    def create: Action[Unit] = action(s => (s, ddl.create(s)))
  }

  // TABLE
  implicit class Table0[A](val t: E#Table[A]) {
    def lift = LiftedTable(t)
  }

  case class LiftedTable[A](t: E#Table[A]) {
    def insert(a: A) = action(s => (s, t.insert(a)(s)))
    def insertAll(as: A*) = action(s => (s, t.insertAll(as: _*)(s)))
  }

  // ACTION
  implicit class Ops[A](a: Action[A]) {
    def apply(db: Database) = {
      val a0 = for {
        s <- IO(db.createSession).liftIO[Action]
        _ <- action(_ => (s, ()))
        x <- a
        _ <- IO(s.close()).liftIO[Action] // TODO: bracket properly
      } yield x
      eval(a0, null).map(_._2)
    }
  }

  // LIFTIO
  implicit class LiftIO[A](a: IO[A]) {
    def lift = a.liftIO[Action]
  }

}



