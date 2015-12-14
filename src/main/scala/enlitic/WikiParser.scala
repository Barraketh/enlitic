package enlitic

import org.jsoup.Jsoup
import scala.collection.JavaConversions._

object WikiParser {
  val WIKI_URL = "https://en.wikipedia.org"

  def fullUrl(relUrl: String) = WIKI_URL + relUrl

  def pageName(relUrl: String) = relUrl.split("/").last

  def getPageUrls(categoryBody: String): List[String] = {
    Jsoup.parse(categoryBody)
      .getElementById("mw-pages")
      .getElementsByClass("mw-category-group").toList
      .flatMap(_.getElementsByTag("a").toList)
      .map(_.attr("href"))
  }

  def getNextPageUrl(categoryBody: String): Option[String] = {
    val doc = Jsoup.parse(categoryBody)
    doc.select("a:contains(next page)").toList.map(_.attr("href")).headOption
  }

  def getPageText(pageBody: String): String = {
    Jsoup.parse(pageBody)
      .getElementById("mw-content-text")
      .text()
  }
}
