package sketch.scope.pdf

import cats.Monad
import sketch.scope.measure.Measure

import scala.language.higherKinds

/**
  * Licensed by Probe Technology, Inc.
  */
trait Dist[A] {

  def measure: Measure[A]

}

trait DistPropOps[D[_]<:Dist[_]] extends DistPropLaws[D] {

  def probability[A](dist: D[A], from: A, to: A): Option[Double]

  //  def pdf(sketch: S, a: Double): Option[Double]

  //  def cdf(sketch: S, a: Double): Option[Double]

  def sample[A](dist: D[A]): (D[A], A)

}

trait DistPropLaws[D[_]<:Dist[_]] { self: DistPropOps[D] =>

  def samples[A](dist: D[A], n: Int): (D[A], List[A]) = {
    (0 until n).foldLeft[(D[A], List[A])]((dist, Nil)){ case ((utdDist1, acc), _) =>
      val (utdDist2, s) = sample(utdDist1)
      (utdDist2, s :: acc)
    }
  }

}

object Dist extends DistPropOps[Dist] {

  def delta[A](implicit measure: Measure[A]): Dist[A] = DeltaDist(measure, 0)

  def delta[A](center: A)(implicit measure: Measure[A]): Dist[A] = DeltaDist(measure, measure(center))

  def probability[A](dist: Dist[A], from: A, to: A): Option[Double] = dist match {
    case smooth: SmoothDist[A] => SmoothDist.probability(smooth, from, to)
    case sampled: SampledDist[A] => SampledDist.probability(sampled, from, to)
  }

  def sample[A](dist: Dist[A]): (Dist[A], A) = dist match {
    case smooth: SmoothDist[A] => SmoothDist.sample(smooth)
    case sampled: SampledDist[A] => SampledDist.sample(sampled)
  }

}


