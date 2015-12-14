package enlitic

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


case class CategoryDownloader(internalName: String, wikiName: String) {

  case class Page(name: String, text: String)

  val INDEX_NAME = "index.txt"

  val indexCache = new StringFileCache(Config.DOWNLOAD_CACHE + "/" + internalName)
  val htmlPageCache = new StringFileCache(Config.DOWNLOAD_CACHE + "/" + internalName)
  val txtPageCache = new StringFileCache(Config.TRAINING_DIR + "/" + internalName)

  private def getIndex: List[String] = {
    indexCache.get(INDEX_NAME) {
      downloadIndex
        .flatMap(WikiParser.getPageUrls)
        .mkString("\n")
    }.split("\n").toList
  }

  private def downloadIndex: List[String] = {
    def download(relUrl: String, suffix: Int = 0): List[String] = {
      val body = UrlUtils.downloadAndCache(WikiParser.fullUrl(relUrl), internalName + suffix + ".html", indexCache)
      WikiParser.getNextPageUrl(body) match {
        case Some(nextUrl) => body :: download(nextUrl, suffix + 1)
        case None => List(body)
      }
    }

    download(wikiName, 0)
  }

  private def getPage(url: String): Page = {
    val name = WikiParser.pageName(url)
    val text = txtPageCache.get(name + ".txt") {
      val body = UrlUtils.downloadAndCache(WikiParser.fullUrl(url), name + ".html", htmlPageCache)
      WikiParser.getPageText(body)
    }

    Page(name, text)
  }

  def getPages: Future[List[Page]] = {
    for {
      index <- Future(getIndex)
      pageFutures = index.map(url => Future(getPage(url)))
      pages <- Future.sequence(pageFutures)
    } yield pages
  }
}

object CategoryDownloader {
  val categories = List(
    "Rare Diseases" -> "/wiki/Category:Rare_diseases",
    "Infectious Diseases" -> "/wiki/Category:Infectious_diseases",
    "Cancer" -> "/wiki/Category:Cancer",
    "Congenital Diseases" -> "/wiki/Category:Congenital_disorders",
    "Organs" -> "/wiki/Category:Organs_(anatomy)",
    "Machine Learning Algorithms" -> "/wiki/Category:Machine_learning_algorithms",
    "Medical Devices" -> "/wiki/Category:Medical_devices"
  ).map {
    case (internal, external) => CategoryDownloader(internal, external)
  }

  def main(args: Array[String]): Unit = {
    val result = Future.sequence(categories.map(_.getPages))
    Await.result(result, 10 minutes)
  }
}
