package flip.plot

import cats.data.NonEmptyList
import flip.pdf.{Count, Prim}
import flip.range.RangeP

import scala.collection.immutable.{TreeMap, TreeSet}
import scala.language.postfixOps

trait PointPlot extends Plot {

  def records: Array[(Double, Double)]

  lazy val index: TreeMap[Double, Int] = {
    var i = 0
    val arr = Array.ofDim[(Double, Int)](records.length)
    while (i < records.length) {
      val (x, _) = records.apply(i)
      arr.update(i, (x, i))
      i += 1
    }
    TreeMap(arr: _*)
  }

  override def toString: String = "PointPlot(" + records.map { case (x, y) => s"$x -> $y" }.mkString(", ") + ")"

}

trait PointPlotOps[P <: PointPlot] extends PlotOps[P] with PointPlotLaws[P] {

  def modifyRecords(plot: P, f: Array[(Double, Double)] => Array[(Double, Double)]): P

}

trait PointPlotLaws[P <: PointPlot] { self: PointPlotOps[P] =>

  def interpolation(plot: P, x: Double): Double = {
    val records = plot.records
    plot.index.from(x).headOption.fold(0.0) {
      case (x2, i2) =>
        if (i2 > 0) {
          val (x1, y1) = records(i2 - 1)
          val (x2, y2) = records(i2)
          Fitting((x1, y1) :: (x2, y2) :: Nil, x).getOrElse(0.0)
        } else 0.0
    }
  }

  /**
    * @param i Referencial index for the records of the plot.
    * */
  def referencialInterpolation(plot: P, x: Double, i: Int): Double = {
    val records = plot.records
    def refs(j: Int): Option[List[(Double, Double)]] =
      if (i + j < records.length && i + j - 1 >= 0) {
        Some(records.apply(i + j - 1) :: records.apply(i + j) :: Nil)
      } else if (i + j - 1 < records.length && i + j - 2 >= 0) {
        Some(records.apply(i + j - 2) :: records.apply(i + j - 1) :: Nil)
      } else None

    (refs(1).flatMap(refs1 => Fitting(refs1, x)) orElse
      refs(0).flatMap(refs0 => Fitting(refs0, x)))
      .getOrElse(interpolation(plot, x))
  }

  def add(plots: NonEmptyList[(Double, P)]): P =
    modifyRecords(
      plots.head._2,
      _ => {
        val _plots: Array[(Double, P)] = plots.toList.toArray
        val size = plots.toList.map { case (_, plot) => plot.records.length }.sum

        val records1 = Array.ofDim[(Double, Double)](size)
        val idxs = Array.fill(_plots.length)(0)
        var i = 0

        while (i < size) {
          var j = 0
          var xMin = Double.MaxValue
          var minIdx = 0
          while (j < _plots.length) {
            val (_, plot) = _plots.apply(j)
            val x = if (idxs(j) < plot.records.length) plot.records.apply(idxs(j))._1 else Double.MaxValue
            if (x < xMin) {
              xMin = x
              minIdx = j
            }
            j += 1
          }

          var k = 0
          var y2 = 0.0
          while (k < _plots.length) {
            val (w, _plot) = _plots.apply(k)
            val idx = idxs(k)
            val ref = if (idx < _plot.records.length) idx else _plot.records.length - 1
            y2 += w * referencialInterpolation(_plot, xMin, ref)
            k += 1
          }

          records1.update(i, (xMin, y2))
          idxs.update(minIdx, idxs(minIdx) + 1)
          i += 1
        }

        records1
      }
    )

  def inverse(plot: P): P =
    modifyRecords(
      plot,
      (records0: Array[(Double, Double)]) => {
        var i = 0
        val records1 = Array.ofDim[(Double, Double)](records0.length)
        while (i < records0.length) {
          val (x, y) = records0(i)
          records1.update(i, (y, x))
          i += 1
        }
        records1
      }
    )

  def normalizedCumulative(plot: P): P =
    modifyRecords(
      plot,
      (records0: Array[(Double, Double)]) => {
        var (i, cum) = (0, 0.0)
        var (x1, y1) = (Double.NaN, Double.NaN)
        val sum = integralAll(plot)
        val records1 = Array.ofDim[(Double, Double)](records0.length)
        while (i < records0.length) {
          val (x2, y2) = records0(i)
          cum += (if(!x1.isNaN && !y1.isNaN) areaPoint(x1, y1, x2, y2) else 0.0) / sum
          records1.update(i, (x2, cum))
          x1 = x2
          y1 = y2
          i += 1
        }
        records1
      }
    )

  def inverseNormalizedCumulative(plot: P): P =
    modifyRecords(
      plot,
      (records0: Array[(Double, Double)]) => {
        var (i, cum) = (0, 0.0)
        var (x1, y1) = (Double.NaN, Double.NaN)
        val sum = integralAll(plot)
        val records1 = Array.ofDim[(Double, Double)](records0.length)
        while (i < records0.length) {
          val (x2, y2) = records0(i)
          cum += (if(!x1.isNaN && !y1.isNaN) areaPoint(x1, y1, x2, y2) else 0.0) / sum
          records1.update(i, (cum, x2))
          x1 = x2
          y1 = y2
          i += 1
        }
        records1
      }
    )

  def integralAll(plot: P): Double = {
    val records = plot.records
    var acc = 0.0
    var (x1, y1) = (Double.NaN, Double.NaN)
    var i = 0
    while (i < records.length) {
      val (x2, y2) = records.apply(i)
      acc += (if(!x1.isNaN && !y1.isNaN) areaPoint(x1, y1, x2, y2) else 0.0)
      x1 = x2
      y1 = y2
      i += 1
    }
    acc
  }

  def areaPoint(x1: Double, y1: Double, x2: Double, y2: Double): Double = {
    if (y1 == 0 && y2 == 0) 0
    else RangeP(x1, x2).roughLength * (y2 / 2 + y1 / 2)
  }

}

object PointPlot extends PointPlotOps[PointPlot] {

  private case class PointPlotImpl(records: Array[(Double, Double)]) extends PointPlot

  def apply(records: Array[(Double, Double)]): PointPlot = safe(records)

  def unsafe(records: Array[(Double, Double)]): PointPlot = PointPlotImpl(records)

  def safe(records: Array[(Double, Double)]): PointPlot = unsafe(records.sortBy(_._1))

  def empty: PointPlot = unsafe(Array.empty[(Double, Double)])

  def squareKernel(ds: List[(Prim, Count)], window: Double): PointPlot = {
    val sum = ds.map(d => d._2).sum
    val _window = if (window <= 0) 1e-100 else window
    val dsArr = ds.toArray
    val records = Array.ofDim[(Double, Double)](dsArr.length * 2)

    var i = 0
    while(i < dsArr.length) {
      val (value, count) = dsArr.apply(i)
      val x1 = value - (_window / 2)
      val x2 = value + (_window / 2)
      val y = if (sum * _window > 0) count / (sum * _window) else 0
      records.update(i * 2, (x1, y))
      records.update(i * 2 + 1, (x2, y))
      i += 1
    }

    unsafe(records)
  }

  def modifyRecords(plot: PointPlot, f: Array[(Double, Double)] => Array[(Double, Double)]): PointPlot =
    unsafe(f(plot.records))

}
