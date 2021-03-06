package flip.conf.pdf

import flip.pdf.SelectiveSketch

trait SelectiveSketchConfB[+D <: SelectiveSketch[_]] extends PeriodicSketchConfB[D] {

  val rebuildThreshold: Double

}
