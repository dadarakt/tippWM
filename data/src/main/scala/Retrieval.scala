/**
 * Created by Jannis on 5/19/14.
 * Collects all needed data from the web and the responses from players.
 */

import org.jsoup.Jsoup
import scalaj.http.Http
import scalaj.http.HttpOptions
import scala.collection.JavaConversions._


object Retrieval {

//  // Finds all the games for the vorrunde
 // def getAllGamesVorrunde = { List[Match]() }


  // Helper to make the online queries more readable
  def getDataOnline(query: scala.xml.Elem) = {
    val rawData = Http.postData("http://www.openligadb.de/Webservices/Sportsdata.asmx", query.toString)
      .header("Host", "www.openligadb.de")
      .header("Content-Type", "application/soap+xml; charset=utf-8")
      .header("Content-Length", "10000")
      .option(HttpOptions.connTimeout(1000))
      .option(HttpOptions.readTimeout(10000))
      .asString
    Jsoup.parse(rawData)
  }


//
}


