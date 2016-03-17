package scalaz


/**
  * Created by logicalguess on 3/16/16.
  */

final case class Dag[A](value: A, dependencies: IList[Dag[A]]){
  def toTree(implicit A: Order[A]): DagTree[A] =
    DagTree(value, dependencies.map(_.toTree))
}

object Dag {
  def single[A](a: A): Dag[A] = Dag(a, INil())

  def dag[A](a: A, dependencies: Dag[A]*): Dag[A] =
    Dag(a, IList(dependencies: _*))
}

final case class DagTree[A](root: A, nodes: IList[DagTree[A]]) {
  def map[B](f: A => B): DagTree[B] =
    DagTree(f(root), nodes.map(_.map(f)))

  def flatMap[B](f: A => DagTree[B]): DagTree[B] = {
    val r = f(root)
    DagTree(r.root, r.nodes ::: nodes.map(_.flatMap(f)))
  }

  def toZTree: Tree[A] =
    Tree.node(root, nodes.map(_.toZTree).toStream)

  def show(implicit A: Show[A]): String =
    toZTree.drawTree

  override def toString: String =
    show(Show.showA)

  def all: IList[A] = {
    def squish(tree: DagTree[A], xs: IList[A]): IList[A] =
      tree.root :: Foldable[IList].foldRight(tree.nodes, xs)(squish(_, _))

    squish(this, IList.empty)
  }

  def traverse1[G[_], B](f: A => G[B])(implicit G: Apply[G]): G[DagTree[B]] =
    nodes match {
      case INil() =>
        G.map(f(root))(DagTree.single)
      case ICons(x, xs) =>
        G.apply2(f(root), NonEmptyList.nel(x, xs.toList).traverse1(_.traverse1(f))) {
          case (h, t) => DagTree(h, IList.fromList(t.list))
        }
    }

  def distinct(implicit A: Order[A]): DagTree[A] = {
    val x = Traverse[DagTree].traverseS[ISet[A], A, Maybe[A]](this){ a =>
      for{
        set <- State.get[ISet[A]]
        s <- if(set.contains(a)){
          State.state[ISet[A], Maybe[A]](Maybe.empty[A])
        }else {
          State((_: ISet[A]).insert(a) -> Maybe.just(a))
        }
      } yield s
    }.run(ISet.empty)._2

    def loop(t: DagTree[Maybe[A]]): Maybe[DagTree[A]] = t match{
      case DagTree(Maybe.Just(a), b) =>
        Maybe.just(
          DagTree(a, MonadPlus[IList].unite(b.map(loop)))
        )
      case _ =>
        Maybe.empty
    }

    loop(x).toOption.get
  }
}

object DagTree {

  implicit val instance: Monad[DagTree] with Traverse1[DagTree] =
    new Monad[DagTree] with Traverse1[DagTree] {
      override def traverse1Impl[G[_], A, B](fa: DagTree[A])(f: A => G[B])(implicit G: Apply[G]) =
        fa.traverse1(f)

      override def foldMapRight1[A, B](fa: DagTree[A])(z: A => B)(f: (A, => B) => B) =
        (fa.all.reverse: @unchecked) match {
          case ICons(h, t) => t.foldLeft(z(h))((b, a) => f(a, b))
        }

      override def point[A](a: => A) =
        DagTree.single(a)

      override def bind[A, B](fa: DagTree[A])(f: A => DagTree[B]): DagTree[B] =
        fa.flatMap(f)
    }

  def single[A](a: A): DagTree[A] = DagTree(a, IList.empty)

}

object Main extends App {
  val functor = Dag.dag("functor")
  val cobind = Dag.dag("cobind", functor)
  val apply = Dag.dag("apply", functor)
  val applicative = Dag.dag("applicative", apply)
  val bind = Dag.dag("bind", apply)
  val monad = Dag.dag("monad", bind, applicative)
  val foldable = Dag.dag("foldable")
  val traverse = Dag.dag("traverse", foldable, functor)
  val plus = Dag.dag("plus")
  val plusEmpty = Dag.dag("plusEmpty", plus)
  val applicativePlus = Dag.dag("applicativePlus", applicative, plusEmpty)
  val monadPlus = Dag.dag("monadPlus", applicativePlus, monad)
  val isEmpty = Dag.dag("isEmpty", plusEmpty)

  import scalaz.std.string._

  val t = Dag.dag("maybe", monadPlus, cobind, traverse, isEmpty).toTree
  println(t)
  println(t.distinct)

}