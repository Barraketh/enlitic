package enlitic

/**
  * Created by ptsier on 12/11/15.
  */
object Paths {
  def createCache(m: Int, n: Int) = {
    val cache = new Array[Array[Int]](m)
    for (i <- 0 until m) {
      cache(i) = new Array[Int](n)
    }

    for {
      i <- 0 until m
      j <- 0 until n
    } {
      cache(i)(j) = -1
    }

    cache
  }


  def p(m: Int, n: Int): Int = {
    val cache: Array[Array[Int]] = createCache(m, n)

    def pRec(mr: Int, nr: Int): Int = {
      if (mr == 1 || nr == 1) 1
      else if (cache(mr - 1)(nr - 1) != -1) cache(mr - 1)(nr - 1)
      else {
        val res = pRec(mr - 1, nr) + pRec(mr, nr - 1)
        cache(mr - 1)(nr - 1) = res
        res
      }
    }

    pRec(m, n)
  }

  def main(args: Array[String]): Unit = {
    println(p(2, 2))
  }

}
