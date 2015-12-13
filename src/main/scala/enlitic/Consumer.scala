package enlitic

import java.util.concurrent.BlockingQueue

/**
  * Created by ptsier on 12/11/15.
  */
abstract class Consumer[IN, OUT](in: BlockingQueue[IN], out: BlockingQueue[OUT]) extends Runnable {
  var kill = false

  override def run(): Unit = {
    while (!kill) {

    }
  }

  def consume()
}
