package enlitic

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}

object SparkHelper {
  def getLocalContext = {
    val conf = new SparkConf()
      .setAppName("Enlitic Pipeline")
      .setMaster("local[2]")
      .set("spark.executor.memory", "4g")

    val sparkContext = new SparkContext(conf)
    new SQLContext(sparkContext)
  }
}
