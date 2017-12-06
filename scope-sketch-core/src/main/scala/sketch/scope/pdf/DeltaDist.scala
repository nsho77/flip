package sketch.scope.pdf

import sketch.scope.measure.Measure
import sketch.scope.range._

/**
  * Licensed by Probe Technology, Inc.
  *
  * Dirac Delta Function.
  */
case class DeltaDist[A](measure: Measure[A], pole: Prim) extends SmoothDist[A]

trait DeltaDistOps extends SmoothDistPropOps[DeltaDist] {

  def probability[A](dist: DeltaDist[A], from: A, to: A): Option[Double] = {
    if(RangeP(dist.measure.to(from), dist.measure.to(to)).contains(dist.pole)) Some(1) else Some(0)
  }

  def sample[A](dist: DeltaDist[A]): (DeltaDist[A], A) = (dist, dist.measure.from(dist.pole))

}

object DeltaDist extends DeltaDistOps