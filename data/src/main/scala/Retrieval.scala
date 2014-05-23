/**
 * Created by Jannis on 5/19/14.
 * Collects all needed data from the web and the responses from players.
 */

import java.text.SimpleDateFormat
import java.util.TimeZone
import org.jsoup.Jsoup
import scala.xml.MalformedAttributeException
import scalaj.http.Http
import scalaj.http.HttpOptions
import scala.collection.JavaConversions._


object Retrieval {

//  val leagueShortcut = "WM-2014"
  val leagueShortcut = "test-wm"
  val leagueID = 676
  val season = 2014

  def getAllTeams = {
    // Get all the teams:
    val teamsRaw = getDataOnline(
      <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <GetTeamsByLeagueSaison xmlns="http://msiggi.de/Sportsdata/Webservices">
            <leagueShortcut>{leagueShortcut}</leagueShortcut>
            <leagueSaison>{2014}</leagueSaison>
          </GetTeamsByLeagueSaison>
        </soap12:Body>
      </soap12:Envelope>
    )

    // mapping for the groups:
    def groupName(pos: Int) = {
      if(pos < 4) 'A'
      else if(pos < 8) 'B'
      else if(pos < 12) 'C'
      else if(pos < 16) 'D'
      else if(pos < 20) 'E'
      else if(pos < 24) 'F'
      else if(pos < 28) 'G'
      else if(pos < 32) 'H'
      else throw new RuntimeException("Error in the number of teams")
    }

      (for {
      (team, pos) <- teamsRaw.getElementsByTag("team").zipWithIndex
    } yield  new Team(
        team.getElementsByTag("teamname").text,
        team.getElementsByTag("teamid").text.toInt,
        groupName(pos),
        team.getElementsByTag("teamiconurl").text
      )).toList
  }

  // Finds all the games for the vorrunde
  def getAllGamesVorrunde = {
    // First get the groupOrderId for further processing
    val result = getDataOnline(
      <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <GetAvailGroups xmlns="http://msiggi.de/Sportsdata/Webservices">
            <leagueShortcut>{leagueShortcut}</leagueShortcut>
            <leagueSaison>{season}</leagueSaison>
          </GetAvailGroups>
        </soap12:Body>
      </soap12:Envelope>)

    val groupOrderIds = result.getElementsByTag("grouporderid").text.split(' ').toList

    val groupOrderId = groupOrderIds(0)
    // Then go on to find the matches for the group
    val gamesRaw = getDataOnline(
      <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <GetMatchdataByGroupLeagueSaison xmlns="http://msiggi.de/Sportsdata/Webservices">
            <groupOrderID>{groupOrderId}</groupOrderID>
            <leagueShortcut>{leagueShortcut}</leagueShortcut>
            <leagueSaison>{season}</leagueSaison>
          </GetMatchdataByGroupLeagueSaison>
        </soap12:Body>
      </soap12:Envelope>
    ).getElementsByTag("matchdata")

    // Usa a nice date format
    val dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dateformat.setTimeZone(TimeZone.getTimeZone("utc"))

    // Get the games as the datastructure and return them
    (for {
      game <- gamesRaw
      groupName = Team.allTeams.filter(_.name == game.getElementsByTag("nameteam1").text).head.group

    } yield {
        val isFinished = game.getElementsByTag("matchisfinished").text.toBoolean
        val (score1, score2) = if(isFinished){
          (game.getElementsByTag("pointsteam1").first.text.toInt, game.getElementsByTag("pointsteam2").first.text.toInt)
        } else {
          (-1, -1)
        }
        new Match(
          game.getElementsByTag("nameteam1").text,
          game.getElementsByTag("idteam1").text.toInt,
          game.getElementsByTag("nameteam2").text,
          game.getElementsByTag("idteam2").text.toInt,
          groupName,
          dateformat.parse(game.getElementsByTag("matchdatetimeutc").text),
          game.getElementsByTag("locationcity").text,
          game.getElementsByTag("locationid").text.toInt,
          game.getElementsByTag("locationstadium").text,
          game.getElementsByTag("matchid").text.toInt,
          game.getElementsByTag("groupid").text.toInt,
          game.getElementsByTag("grouporderid").text.toInt,
          game.getElementsByTag("groupname").text,
          score1,
          score2,
          game.getElementsByTag("matchisfinished").text.toBoolean)
      }
    ).toList
  }


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


  def getAllPlayers = {
    val filename = "src/main/resources/responses.csv"
    val rawData = scala.io.Source.fromFile(filename).getLines
    val header = rawData.next

    // parses a line from the file which represents a player
    def parseSingleResponse(response: List[String]): Player = {

      println(response.length)

      val firstName = response(1)
      val lastName  = response(2)
      val nickName  = response(3)
      val email     = response(4)

      // The initial tipps for the winners of the cup
      val first   = Team.allTeams.find(_.name == (response(response.length - 3))) match {
        case Some(t) => t
        case None =>
          throw new MalformedAttributeException(s"The name of the first placed team for $email is unknown!")
      }
      val second  = Team.allTeams.find(_.name == (response(response.length - 2))) match {
        case Some(t) => t
        case None =>
          throw new MalformedAttributeException(s"The name of the second placed team for $email is unknown!")
      }
      val third   = Team.allTeams.find(_.name == (response(response.length - 1))) match {
        case Some(t) => t
        case None =>
          throw new MalformedAttributeException(s"The name of the third placed team for $email is unknown!")
      }

      val tipps = for{
        (tipp , index)<- response.drop(5).take(48).zipWithIndex
        tipps = tipp.split(':').toList.map(_.toInt)
      } yield(new Tipp(
          Match.allMatches(index).id,
          firstName + lastName + nickName,
          tipps(0),
          tipps(1)
        ))
      new Player(firstName, lastName, nickName, email, tipps.toList, first, second, third)
    }

    (for{
      responseLine <- rawData
      response = responseLine.split(',').toList if(!response.isEmpty && response(0) != "")
    } yield(parseSingleResponse(response))).toList

  }
}


