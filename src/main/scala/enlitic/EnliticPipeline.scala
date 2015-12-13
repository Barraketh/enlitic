package enlitic

import enlitic.WikiCategory.LabeledDocument
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{Classifier, DecisionTreeClassifier, RandomForestClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, StringIndexer, Tokenizer}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel, ParamGridBuilder}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{DataFrame, Row}

import scala.io.Source

/**
  * Created by ptsier on 12/12/15.
  */
object EnliticPipeline {
  val MODEL_NAME = "cvModel_dt"
  val MODEL_STORE = Config.DATA_DIR + "/model"

  val modelCache = new ObjectFileCache[EnliticModel](MODEL_STORE)

  case class EnliticModel(model: CrossValidatorModel, mapping: Map[String, Double])

  def randomForestClassifier = {
    val rf = new RandomForestClassifier()
      .setNumTrees(10)
      .setMaxDepth(20)

    val paramGrid = new ParamGridBuilder()
      .addGrid(rf.numTrees, Array(5, 10, 50, 100))
      .addGrid(rf.maxDepth, Array(5, 10, 20))
      .build()

    (rf, paramGrid)
  }

  def decisionTreeClassifier = {
    val dt = new DecisionTreeClassifier()

    val paramGrid = new ParamGridBuilder()
      .addGrid(dt.maxDepth, Array(5, 10, 20))
      .build()

    (dt, paramGrid)
  }

  private def trainModel(training: DataFrame, classifier: => (Classifier[_, _, _], Array[ParamMap])): EnliticModel = {
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")

    val indexer = new StringIndexer()
      .setInputCol("category")
      .setOutputCol("label")

    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")

    val (cl, paramGrid) = classifier

    val pipeline = new Pipeline()
      .setStages(Array(indexer, tokenizer, hashingTF, cl))

    val cv = new CrossValidator()
      .setEstimator(pipeline)
      .setEvaluator(new MulticlassClassificationEvaluator())
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(2)

    val labelMapping: Map[String, Double] = indexer.fit(training)
      .transform(training.select("category"))
      .distinct()
      .collect()
      .map {
        case Row(category: String, label: Double) => category -> label
      }.toMap

    val model = cv.fit(training)

    EnliticModel(model, labelMapping)
  }

  def testModel(model: EnliticModel, test: DataFrame, hashedDocuments: Map[Long, LabeledDocument]) {
    val predictionAndLabels = model.model.transform(test)
      .select("id", "prediction")
      .map {
        case Row(id: Long, prediction: Double) =>
          val doc = hashedDocuments(id)
          (prediction, model.mapping(doc.category))
      }

    val metrics = new MulticlassMetrics(predictionAndLabels)
    println("Precision: " + metrics.precision)
    println("Recall: " + metrics.recall)
    println("F1 score: " + metrics.fMeasure)
  }

  def trainAndTest(classifier: => (Classifier[_, _, _], Array[ParamMap])): Unit = {
    val context = SparkHelper.getLocalContext
    val labeledDocuments = WikiCategory.getLabeledDocuments
    val hashedLabledDocuments = labeledDocuments.map(l => l.id -> l).toMap

    val split = context.createDataFrame(
      labeledDocuments
    ).toDF("id", "category", "name", "text")
      .randomSplit(Array(0.9, 0.1), 0)

    val training = split(0).cache()
    val test = split(1).select("id", "text")

    val model = modelCache.get(MODEL_NAME)(trainModel(training, decisionTreeClassifier))
    testModel(model, test, hashedLabledDocuments)
  }

  def main(args: Array[String]): Unit = {
    trainAndTest(decisionTreeClassifier)
  }

}

object WikiClassifier {
  def main(args: Array[String]): Unit = {
    val text = Source.fromURL(args(0)).getLines().mkString("\n")
    val model = EnliticPipeline.modelCache.getOpt(EnliticPipeline.MODEL_NAME).getOrElse {
      throw new RuntimeException("Model not built.  Please run the command sbt 'runMain enlitic.EnliticPipeline'")
    }

    val context = SparkHelper.getLocalContext
    val df = context.createDataFrame(List(0 -> text)).toDF("id", "text")

    val probabilities =  model.model.transform(df)
        .select("probability")
        .collect()
        .map {
          case Row(probability: Vector) => probability.toArray.toList.zipWithIndex
        }.head

    val labelToCategory = model.mapping.map {
      case (cat, label) => (label, cat)
    }

    probabilities.foreach {
      case (prob, idx) => println(labelToCategory(idx) + ": " + prob)
    }
  }
}
