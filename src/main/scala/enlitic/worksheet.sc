import enlitic.WikiCategory
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}

val labeledDocuments = WikiCategory.getLabeledDocuments
labeledDocuments.groupBy(_.category).map {
  case (cat, docs) => cat -> docs.length
}

val conf = new SparkConf()
  .setAppName("Enlitic Pipeline")
  .setMaster("local[2]")
  .set("spark.executor.memory", "1g")

val sparkContext = new SparkContext(conf)
val sqlContext = new SQLContext(sparkContext)


val split = sqlContext.createDataFrame(
  labeledDocuments.map { l => (l.id, l.text, l.category)}
).toDF("id", "text", "category")
  .randomSplit(Array(0.9, 0.1))

