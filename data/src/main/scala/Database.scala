import java.text.SimpleDateFormat
import java.util.TimeZone
import scala.util.control.NonFatal
import java.sql.DriverManager
import scala.collection.JavaConversions._
import scala.xml.MalformedAttributeException
import scala.pickling._
import json._

/**
 * Created by Jannis on 5/23/14.
 * Loads all the data and writes it to the database
 */

object Database {

  // credentials
  val url   = "jdbc:mysql://localhost/tippwm"
  val user  = "Jannis"
  val pw    = ""

  // for retrieving the data
  val leagueShortcut = "test-wm"
  val leagueID = 676
  val season = 2014

  // Instantiate the driver
  val driver = "com.mysql.jdbc.Driver"
  Class.forName(driver).newInstance



  // Initializes the tables into the given database, can only be done online
  def main(args: Array[String])= {
    // The database
    initializePlayers
    initializeTeams
    initializeMatches
  }


//  def printTable(connData: ConnData, table: String) = {
//    val conn = DriverManager.getConnection(connData.url, connData.username, connData.password)
//    try {
//      val statement = conn.createStatement()
//      val result = statement.executeQuery(s"select * from $table")
//
//      while (result.next()) {
//        println(s"" +
//          s"${result.getString("firstname")}, ${result.getString("lastname")}, ${result.getString("nickname")}")
//        println(result.getString("tipps").unpickle[List[String]])
//      }
//    } catch {
//      case ex: scala.pickling.PicklingException =>   println(s"Error while deserialization, $ex")
//      case NonFatal(ex) => println(s"Could not connect to DB, $ex")
//    } finally {
//      conn.close()
//    }
//  }

  def initializeMatches: Unit = {

    // There will be a set of groups from Vorrunde to Final
    val groupsRaw = Retrieval.getDataOnline(
      <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <GetAvailGroups xmlns="http://msiggi.de/Sportsdata/Webservices">
            <leagueShortcut>{leagueShortcut}</leagueShortcut>
            <leagueSaison>{season}</leagueSaison>
          </GetAvailGroups>
        </soap12:Body>
      </soap12:Envelope>)

    val groupOrderIds = groupsRaw.getElementsByTag("grouporderid").text.split(' ').toList
    val groupOrderId = groupOrderIds(0)

    // Then go on to find the matches for the group
    val gamesRaw = Retrieval.getDataOnline(
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

    // use date formats to parse from raw and then to save it to mysql
    val rawDateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    rawDateformat.setTimeZone(TimeZone.getTimeZone("utc"))
    val sqlDateformat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    // Get the games as the datastructure and return them
    val conn = DriverManager.getConnection(url, user, pw)
    val statement = conn.createStatement

    for (game <- gamesRaw) {
      val team1 = game.getElementsByTag("nameteam1").text
      val team2 = game.getElementsByTag("nameteam2").text
      val groupName = {
        val conn = DriverManager.getConnection(url, user, pw)
        val statement = conn.createStatement()
        val result = statement.executeQuery(s"select groupchar from team where name='$team1'")
        result.next()
        val c = result.getString("groupchar")
        conn.close
        c
      }
      val isFinished = game.getElementsByTag("matchisfinished").text.toBoolean
      val (score1, score2) = if(isFinished){
        (game.getElementsByTag("pointsteam1").first.text.toInt,
         game.getElementsByTag("pointsteam2").first.text.toInt)
      } else {
        (-1, -1)
      }
      val date          = sqlDateformat.format(rawDateformat.parse(game.getElementsByTag("matchdatetimeutc").text))
      val location      = game.getElementsByTag("locationcity").text
      val stadium       = game.getElementsByTag("locationstadium").text
      val onlineId      = game.getElementsByTag("matchid").text.toInt
      val groupId       = game.getElementsByTag("groupid").text.toInt
      val grouporderid  = game.getElementsByTag("grouporderid").text.toInt
      val groupname     = game.getElementsByTag("groupname").text

      try {

        statement.execute(s"INSERT INTO matches VALUES " +
          s"(DEFAULT, '$team1', '$team2', '$groupName', '$date', '$location', '$stadium', '$onlineId', " +
          s"'$groupId', '$grouporderid', '$groupname', '${if(isFinished)1 else 0}', '$score1', '$score2')")
        println(s"Added match $team1 vs. $team2, in group $groupname to the DB with result $team1:$team2")

      } catch {
        case ex: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException =>
          println(s"Match $team1 vs $team2 in group $groupname was already in the DB. Not creating entry!!!")
      }
    }
    conn.close

  }


  def initializeTeams: Unit = {

    // Get all the teams:
    val teamsRaw = Retrieval.getDataOnline(
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

    for {
      (team, pos) <- teamsRaw.getElementsByTag("team").zipWithIndex
      name  = team.getElementsByTag("teamname").text
      group = groupName(pos)
      icon  = team.getElementsByTag("teamiconurl").text
    } {
      // And create his entry
      try {
        val conn = DriverManager.getConnection(url, user, pw)
        val statement = conn.createStatement
        statement.execute(s"INSERT INTO team VALUES (DEFAULT, '$name', '$group', DEFAULT, '$icon', DEFAULT, " +
          s"DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT)")
        println(s"Added team $name, in group $group to the DB.")
        conn.close()

      } catch {
        case ex: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException =>
          println(s"Team $name in group $group was already in the DB. Not creating entry!!!")
      }
    }
  }

  // TODO so far this only loads the players names into a table, not their tipps
  def initializePlayers = {
    val filename = "src/main/resources/responses.csv"
    val rawData = scala.io.Source.fromFile(filename).getLines
    val header = rawData.next

    // parses a line from the file which represents a player
    def parseSingleResponse(response: List[String]): Unit = {

      // Get all the data for one player
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

      // Get all the tipps and save them with their game's online ID
      val tipps = (for{
        (tipp , index)<- response.drop(5).take(48).zipWithIndex
        tipps = tipp.split(':').toList.map(_.toInt)
      } yield new Tipp(
          Match.allMatches(index).id,
          firstName + lastName + nickName,
          tipps(0),
          tipps(1))).pickle.value

      // Save the player to the database
      try {
        val conn = DriverManager.getConnection(url, user, pw)
        val statement = conn.createStatement
        statement.execute(s"INSERT INTO player VALUES (DEFAULT, '$firstName', '$lastName', '$nickName', '$email', " +
          s"'$first', '$second', '$third', '$tipps', DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT," +
          s"DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT)")
        println(s"Added player $firstName, $lastName to the DB.")

      } catch {
        case ex: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException =>
          println(s"Player $firstName, $lastName was already in the DB. Not creating entry!!!")
      }
    }

    // Iterate over all entries coming from the google-form
    for{
      responseLine <- rawData
      response = responseLine.split(',').toList if(!response.isEmpty && response(0) != "")
    } (parseSingleResponse(response))
  }
}
