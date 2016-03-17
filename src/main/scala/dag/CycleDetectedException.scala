package dag

import java.util


class CycleDetectedException(message: String, cycle: util.List[String]) extends Exception {

  def getCycle: util.List[String] = {
    cycle
  }

  def cycleToString: String = {
    val buffer = new StringBuilder
    val iterator = this.cycle.iterator
    while (iterator.hasNext) {
      buffer.append(iterator.next)
      if (iterator.hasNext) {
        buffer.append(" --> ")
      }
    }
    buffer.toString()
  }

  override def getMessage: String = {
    super.getMessage + " " + this.cycleToString
  }
}