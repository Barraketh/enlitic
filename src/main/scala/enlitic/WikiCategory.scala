package enlitic

import org.jsoup.Jsoup

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


case class WikiCategory(internalName: String, wikiName: String) {
  import Config.DATA_DIR

  val INDEX_NAME = "index.txt"
  val WIKI_URL = "https://en.wikipedia.org"

  private def fullUrl(relUrl: String) = WIKI_URL + relUrl

  val indexCache = new StringFileCache(DATA_DIR + "/" + internalName)
  val htmlPageCache = new StringFileCache(s"$DATA_DIR/$internalName/pages")
  val txtPageCache = new StringFileCache(s"$DATA_DIR/$internalName/txt")

  def getIndex: Future[List[String]] = {
    Future(indexCache.get(INDEX_NAME) {
      downloadIndex.flatMap(parseIndex).mkString("\n")
    }.split("\n").toList)
  }

  private def downloadIndex: List[String] = {
    def download(relUrl: String, suffix: Int = 0): List[String] = {
      val body = UrlUtils.download(fullUrl(relUrl), internalName + suffix + ".html", indexCache)
      val doc = Jsoup.parse(body)
      val href = doc.select("a:contains(next page)").toList.map(_.attr("href")).headOption
      href match {
        case Some(nextUrl) => body :: download(nextUrl, suffix + 1)
        case None => List(body)
      }
    }

    download(wikiName, 0)
  }

  private def parseIndex(body: String): List[String] = {
    Jsoup.parse(body)
      .getElementById("mw-pages")
      .getElementsByClass("mw-category-group").toList
      .flatMap(_.getElementsByTag("a").toList)
      .map(_.attr("href"))
  }

  private def pageName(relUrl: String) = relUrl.split("/").last

  private def getPage(url: String): Future[(String, String)] = {
    val name = pageName(url)
    Future(name -> txtPageCache.get(name + ".txt") {
      val body = UrlUtils.download(fullUrl(url), pageName(url) + ".html", htmlPageCache)
      Jsoup.parse(body)
        .getElementById("mw-content-text")
        .text()
    })
  }

  def getPages: Future[List[(String, String)]] = {
    for {
      index <- getIndex
      pageFutures = index.map(url => getPage(url))
      pages <- Future.sequence(pageFutures)
    } yield pages
  }
}

object WikiCategory {
  val categories = List(
    "Rare Diseases" -> "/wiki/Category:Rare_diseases",
    "Infectious Diseases" -> "/wiki/Category:Infectious_diseases",
    "Cancer" -> "/wiki/Category:Cancer",
    "Congenital Diseases" -> "/wiki/Category:Congenital_disorders",
    "Organs" -> "/wiki/Category:Organs_(anatomy)",
    "Machine Learning Algorithms" -> "/wiki/Category:Machine_learning_algorithms",
    "Medical Devices" -> "/wiki/Category:Medical_devices"
  ).map {
    case (internal, external) => WikiCategory(internal, external)
  }

  case class LabeledDocument(id: Long, category: String, name: String, text: String)

  def getLabeledDocuments = {
    println("Downloading WIKI data")
    val labeledDocumentsF = Future.sequence(categories.map { c =>
      c.getPages map { pages =>
        pages.map { page =>
          c.internalName -> page
        }
      }
    })

    val res = Await.result(labeledDocumentsF, 10 minutes)
      .flatten
      .zipWithIndex
      .map {
        case (((category, (name, text)), idx)) => LabeledDocument(idx, category, name, text)
      }

    res

  }
}
