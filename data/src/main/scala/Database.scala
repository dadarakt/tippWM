import java.text.SimpleDateFormat
import java.util.TimeZone
import java.sql.DriverManager
import scala.collection.JavaConversions._
import scala.pickling._
import json._
import scala.util.control.NonFatal

/**
 * Created by Jannis on 5/23/14.
 * Loads all the data and writes it to the database. Also setups the driver.
 * All classes use the credentials from here.
 */

object Database {
  // credentials
  val url   = "jdbc:mysql://localhost/tippwm"
  val user  = "Jannis"
  val pw    = ""

  // for retrieving the data online
//  val leagueShortcut = "WM-2014"
  val leagueShortcut = "test-wm"
  val leagueID = 676
  val season = 2014

  // Instantiate the driver
  val driver = "com.mysql.jdbc.Driver"
  Class.forName(driver).newInstance

  // use date formats to parse from raw and then to save it to mysql
  val rawDateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  rawDateformat.setTimeZone(TimeZone.getTimeZone("utc"))
  val sqlDateformat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  // Initializes the tables into the given database, can only be done online
  def main(args: Array[String]) = {
    // The database
    loadData
//    println(Player.allPlayers.mkString("\n\n"))
//    println(Match.allMatches.mkString("\n\n"))
  }

  def loadData = {
    initializeTeams
    initializeMatches
    initializePlayers
  }


  def eraseAndSetup = {
    import scala.sys.process._
    // Call the scripts to initialize the tables if not there
    println(s" Dropping all tables: ${"mysql" #< "src/main/resources/erase.sql" !}")
  }

  def updateMatches: Unit = {
    val connection = DriverManager.getConnection(url, user, pw)
    val statement = connection.createStatement

    for {
      m <- Match.unfinishedMatches if(m.date.before(new java.util.Date))
      id = m.onlineId
    } {

      println(s"Looking if match ${m.teamA} vs. ${m.teamB} is finished.")
      val gameraw = Retrieval.getDataOnline(
        <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <GetMatchByMatchID xmlns="http://msiggi.de/Sportsdata/Webservices">
            <MatchID>{id}</MatchID>
          </GetMatchByMatchID>
        </soap12:Body>
      </soap12:Envelope>)

      if(gameraw.getElementsByTag("matchisfinished").text.toBoolean) {
        println(s"Match ${m.teamA} vs. ${m.teamB} is finished! Updating...")

        val scoreA = gameraw.getElementsByTag("pointsteam1").first.text.toInt
        val scoreB = gameraw.getElementsByTag("pointsteam2").first.text.toInt


        println(s"updating game ${gameraw.getElementsByTag("nameteam1").text} vs ${gameraw.getElementsByTag("nameteam2").text} to" +
          s"$scoreA : $scoreB")
        statement.execute(s"UPDATE matches SET isfinished='1', scorea='$scoreA', scoreb='$scoreB' where onlineid='$id'")
      }
    }
    val date = sqlDateformat.format(new java.util.Date())
    statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='match'")
    connection.close
  }

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


    // Get the games as the datastructure and return them
    val conn = DriverManager.getConnection(url, user, pw)
    val statement = conn.createStatement

    for (game <- gamesRaw) {
      val team1 = game.getElementsByTag("nameteam1").text
      val team2 = game.getElementsByTag("nameteam2").text
      val groupName = {
        val result = statement.executeQuery(s"select groupchar from team where name='$team1'")
        result.next()
        val c = result.getString("groupchar")
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
        println(s"Added match $team1 vs. $team2, in group $groupname to the DB with result ${score1}:${score2}")
      } catch {
        case ex: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException =>
          println(s"Match $team1 vs $team2 in group $groupname was already in the DB. Not creating entry!!!")
      }
    }
    val date = sqlDateformat.format(new java.util.Date())
    statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='match'")
    conn.close
  }


  def initializeTeams: Unit = {
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

    val conn = DriverManager.getConnection(url, user, pw)
    val statement = conn.createStatement

    // Get all teams
    for {
      (team, pos) <- teamsRaw.getElementsByTag("team").zipWithIndex
      name  = team.getElementsByTag("teamname").text
      onlineId = team.getElementsByTag("teamid").text.toInt
      group = groupName(pos)
      icon  = team.getElementsByTag("teamiconurl").text
    } {
      // And create the entry

      try {

        statement.execute(s"INSERT INTO team VALUES (DEFAULT, '$onlineId', '$name', '$group', DEFAULT, '$icon', DEFAULT, " +
          s"DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT)")
        println(s"Added team $name, in group $group to the DB.")


      } catch {
        case ex: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException =>
          println(s"Team $name in group $group was already in the DB. Not creating entry!!!")
        case ex: java.sql.SQLException =>
          println(s"Could not add team $name in group $group. Not creating entry!!! $ex")
      }
    }
    val date = sqlDateformat.format(new java.util.Date())
    statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='team'")
    conn.close()
  }

  def updateTeams = {
    val connection = DriverManager.getConnection(url, user, pw)
    val statement = connection.createStatement

    var teamMap: Map[String, (Int,Int,Int,Int,Int,Int)] = Map()
    for{
      team <- Team.allTeams
    } teamMap += (team.name -> (0,0,0,0,0,0))

    // Crunch all matches to see how the teams scored
    for{
      m <- Match.playedMatches.filter(_.groupOrderId == 1)
      teamA  = m.teamA
      teamB  = m.teamB
      scoreA = m.scoreA
      scoreB = m.scoreB
      diff = scoreA-scoreB
    } {
     //#played, wins, losses, draw, goalsscored, goalsgotten
      val beforeA = teamMap(teamA)
      val afterA  = ( beforeA._1 + 1,
                      beforeA._2 + (if(diff > 0) 1 else 0),
                      beforeA._3 + (if(diff < 0) 1 else 0),
                      beforeA._4 + (if(diff == 0) 1 else 0),
                      beforeA._5 + scoreA,
                      beforeA._6 + scoreB)

      val beforeB = teamMap(teamB)
      val afterB  = ( beforeB._1 + 1,
                      beforeB._2 + (if(diff < 0) 1 else 0),
                      beforeB._3 + (if(diff > 0) 1 else 0),
                      beforeB._4 + (if(diff == 0) 1 else 0),
                      beforeB._5 + scoreB,
                      beforeB._6 + scoreA)

      teamMap += (teamA -> afterA)
      teamMap += (teamB -> afterB)
    }


    for {
      (team, stats) <- teamMap
    } {
      statement.execute(s"UPDATE team SET " +
        s"gamesplayed='${stats._1}'," +
        s"wins='${stats._2}', " +
        s"losses='${stats._3}', " +
        s"draws='${stats._4}', " +
        s"goalsscored='${stats._5}', " +
        s"goalsgotten='${stats._6}'," +
        s"points='${(stats._2*3 + stats._4)}'" +
        s"WHERE name='${team}'"
      )
    }
    val date = sqlDateformat.format(new java.util.Date())
    statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='team'")

    connection.close()
  }




  def initializePlayers = {
    val filename    = "src/main/resources/responses.csv"
    val rawData     = scala.io.Source.fromFile(filename).getLines
    val header      = rawData.next.split(',').toList

    val allMatches  = Match.allMatches



    // parses a line from the file which represents a player
    def parseSingleResponse(response: List[String]): Unit = {

      val conn = DriverManager.getConnection(url, user, pw)
      val statement = conn.createStatement

      val firstName = response(1)
      val lastName  = response(2)
      val nickName  = response(3)
      val email     = response(4)

      // The initial tipps for the winners of the cup
      val first   = response(response.length - 3)
      val second  = response(response.length - 2)
      val third   = response(response.length - 1)

      // Write the player to the database - if he is not yet in there
      try {
        // Write that out to get the id for the player to sign the tipps
        statement.execute(s"INSERT INTO player VALUES (DEFAULT, '$firstName', '$lastName', '$nickName', '$email', " +
          s"'$first', '$second', '$third', DEFAULT," +
          s"DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT)")
        println(s"Added player $firstName, $lastName to the DB.")
      } catch {
        case ex: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException =>
          println(s"Player $firstName, $lastName was already in the DB. Not creating entry!!!")
      }

      // Update the tipps to the current state from the tabular
      try{
        val dbId = statement.executeQuery(s"select id from player where email='$email'")
        dbId.next
        val iid =  dbId.getInt("id")
        // Get all the tipps and save them with their game's online ID (serizalized)
        val tipps = (for{
          (tipp , index) <- response.drop(5).take(48).zipWithIndex
          tipps = tipp.split(':').toList.map(_.toInt)
          teams = header(index+5).split('-').map(_.trim)
        } yield {
          assert(teams(0) == allMatches(index).teamA && teams(1) == allMatches(index).teamB)
          new Tipp(
            allMatches(index).onlineId,
            iid,
            tipps(0),
            tipps(1))
        }).pickle.value
        statement.execute(s"UPDATE player SET tipps1='${tipps}'WHERE email='${email}'")
      } catch {
        case NonFatal(ex) => {
          println(s"Updating tipps for player $firstName $lastName failed due to $ex")
        }
      }

      // Update the last edit
      val date = sqlDateformat.format(new java.util.Date())
      statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='player'")
      conn.close()
    }


    // Iterate over all entries coming from the google-form
    for{
      responseLine <- rawData
      response = responseLine.split(',').toList if(!response.isEmpty && response(0) != "")
    } (parseSingleResponse(response))
  }

  def updatePlayers = {
    // update the stats to the database
    val connection = DriverManager.getConnection(url, user, pw)
    val statement = connection.createStatement
    for {
      player <- Player.allPlayers
      stats = player.results
    } {
      statement.execute(s"UPDATE player SET " +
        s"scoredMatches='${stats.scoredMatches.pickle.value}'," +
        s"falseMatches='${stats.falseTippMatches.pickle.value}'," +
        s"missedMatches='${stats.missedMatches.pickle.value}'," +
        s"points='${stats.points}'," +
        s"pointstime='${stats.pointsTimeseries.pickle.value}'," +
        s"tendencies='${stats.tendencies}'," +
        s"tendenciestime='${stats.tendenciesTimeseries.pickle.value}'," +
        s"diffs='${stats.diffs}'," +
        s"diffstime='${stats.diffsTimeseries.pickle.value}'," +
        s"hits='${stats.hits}'," +
        s"hitstime='${stats.hitsTimeseries.pickle.value}' " +
        s"WHERE email='${player.email}'")
    }
    val date = sqlDateformat.format(new java.util.Date())
    statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='player'")

    connection.close()
  }
}
