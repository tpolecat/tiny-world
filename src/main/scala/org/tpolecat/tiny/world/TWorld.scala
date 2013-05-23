package org.tpolecat.tiny.world

import language.higherKinds
import scalaz._
import scalaz.Id._
import scalaz.effect.IO
import scalaz.effect.MonadIO

trait TWorld {

  protected type State

  case class ActionT[F[+_], +A](private[TWorld]run: State => F[(State, A)]) {

    def map[C](f: A => C)(implicit F: Functor[F]): ActionT[F, C] =
      new ActionT(s => F.map(run(s))(p => (p._1, f(p._2))))

    def flatMap[C](f: A => ActionT[F, C])(implicit M: Bind[F]): ActionT[F, C] =
      new ActionT(s => M.bind(run(s))(p => f(p._2).run(p._1)))

  }

  object ActionT {

    implicit def monadActionT[M[+_]](implicit M: Monad[M]) =
      new Monad[({ type λ[+ α] = ActionT[M, α] })#λ] {
        def point[A](a: => A): ActionT[M, A] = new ActionT(s => M.point((s, a)))
        override def map[A, B](fa: ActionT[M, A])(f: (A) => B): ActionT[M, B] = fa map f
        def bind[A, B](fa: ActionT[M, A])(f: (A) => ActionT[M, B]): ActionT[M, B] = fa flatMap f
      }

    implicit object MonadIOActionT extends MonadIO[({ type λ[+α] = ActionT[IO, α] })#λ] {
      def point[A](a: => A): ActionT[IO, A] = monadActionT[IO].point(a)
      def bind[A, B](fa: ActionT[IO, A])(f: A => ActionT[IO, B]): ActionT[IO, B] = fa.flatMap(f)
      def liftIO[A](ioa: IO[A]): ActionT[IO, A] = new ActionT(s => ioa.map((s, _)))
    }

  }

  class Lifted[F[+_]](implicit F: Applicative[F]) {

    type Action[+A] = ActionT[F, A]

    protected def action[A](f: State => (State, A)): Action[A] = new ActionT(s => F.point(f(s)))
    protected def effect[A](f: State => A): Action[A] = action(s => (s, f(s)))
    protected def unit[A](a: => A): Action[A] = action(s => (s, a))
    protected final def eval[A](a: ActionT[F, A], s: State) = a.run(s)

  }

}






