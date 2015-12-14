package enlitic

import scalaj.http._

object UrlUtils {
  val MAX_SLEEP = 60000

  def downloadAndCache(url: String, key: String, cache: Cache[String]): String = {
    cache.get(key) {
      downloadUrl(url, 0)
    }
  }

  private def downloadUrl(url: String, sleepMillis: Int): String = {
    if (sleepMillis > MAX_SLEEP) throw new RuntimeException("Could not download url " + url)
    Thread.sleep(sleepMillis)
    val response = Http(url).option(HttpOptions.followRedirects(true)).asString
    if (response.is2xx) response.body
    else {
      val nextSleep =
        if (sleepMillis == 0) 1000
        else sleepMillis * 2

      downloadUrl(url, nextSleep)
    }
  }
}
