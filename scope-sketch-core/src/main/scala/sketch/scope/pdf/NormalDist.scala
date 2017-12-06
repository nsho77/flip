package sketch.scope.pdf

import org.apache.commons.math3.distribution.NormalDistribution
import sketch.scope.measure.Measure
import sketch.scope.rand._
/**
  * Licensed by Probe Technology, Inc.
  */
case class NormalDist[A](measure: Measure[A], mean: Prim, variance: Prim, rng: IRng) extends SmoothDist[A]

trait NormalDistOps extends SmoothDistPropOps[NormalDist] {

  def probability[A](dist: NormalDist[A], start: A, end: A): Option[Double] = {
    val toPrim = dist.measure(end)
    val fromPrim = dist.measure(start)
    val numericDist = new NormalDistribution(dist.mean, dist.variance)

    Some(numericDist.cumulativeProbability(toPrim) - numericDist.cumulativeProbability(fromPrim))
  }

  def modifyRng[A](dist: NormalDist[A], f: IRng => IRng): NormalDist[A]

  def sample[A](dist: NormalDist[A]): (NormalDist[A], A) = {
    val (rng, rand) = dist.rng.next
    val numericDist = new NormalDistribution(dist.mean, dist.variance)

    (modifyRng(dist, _ => rng), dist.measure.from(numericDist.inverseCumulativeProbability(rand)))
  }

}

object NormalDist extends NormalDistOps {

  def apply[A](measure: Measure[A], mean: Prim, variance: Prim): NormalDist[A] =
    NormalDist(measure, mean, variance, IRng(mean.toInt + variance.toInt))

  def modifyRng[A](dist: NormalDist[A], f: IRng => IRng): NormalDist[A] =
    NormalDist(dist.measure, dist.mean, dist.variance, f(dist.rng))

}
