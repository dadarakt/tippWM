import _root_.java.sql.DriverManager
import java.io.FileNotFoundException
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
  val leagueShortcut = "WM-2014"
//  val leagueShortcut = "test-wm"
  val leagueID = 676
  val season = 2014

  // Instantiate the driver
  val driver = "com.mysql.jdbc.Driver"
  Class.forName(driver).newInstance

  // use date formats to parse from raw and then to save it to mysql
  val rawDateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  rawDateformat.setTimeZone(TimeZone.getTimeZone("utc"))
  val sqlDateformat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

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
    // open a connection to the database to update the data
    val connection = DriverManager.getConnection(url, user, pw)
    val statement = connection.createStatement

    val start = System.currentTimeMillis
    // First get all available groupOrderIDs which represent the different stages of the tournament
    val groupOrderIds = Retrieval.getDataOnline(
      <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <GetAvailGroups xmlns="http://msiggi.de/Sportsdata/Webservices">
            <leagueShortcut>{leagueShortcut}</leagueShortcut>
            <leagueSaison>{season}</leagueSaison>
          </GetAvailGroups>
        </soap12:Body>
      </soap12:Envelope>).getElementsByTag("grouporderid").text.split(' ').toList

    println(s"\tFound groups $groupOrderIds")

    val allMatches = Retrieval.getDataOnline(
      <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <GetMatchdataByLeagueSaison xmlns="http://msiggi.de/Sportsdata/Webservices">
            <leagueShortcut>{leagueShortcut}</leagueShortcut>
            <leagueSaison>{season}</leagueSaison>
          </GetMatchdataByLeagueSaison>
        </soap12:Body>
      </soap12:Envelope>
    ).getElementsByTag("matchdata")

    //println(allTheMatches)
    // Get all the matches as a flatted list of matches (xml-elements)
//    val allMatches = groupOrderIds flatMap  { groupOrderId =>
//      Retrieval.getDataOnline(
//        <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
//          <soap12:Body>
//            <GetMatchdataByGroupLeagueSaison xmlns="http://msiggi.de/Sportsdata/Webservices">
//              <groupOrderID>{groupOrderId}</groupOrderID>
//              <leagueShortcut>{leagueShortcut}</leagueShortcut>
//              <leagueSaison>{season}</leagueSaison>
//            </GetMatchdataByGroupLeagueSaison>
//          </soap12:Body>
//        </soap12:Envelope>
//      ).getElementsByTag("matchdata")}

      println("\tTime for retrieval: " + (System.currentTimeMillis - start))
    // Check if the match is already in the database and proceed accordingly
    val matchesWithId = statement.executeQuery(s"SELECT onlineid from matches")

    var idsInDb = List[Int]()
    while(matchesWithId.next){
      //val tull = matchesWithId.getInt("onlineId")
      //println(matchesWithId.getInt("onlineId"))
      idsInDb ::= matchesWithId.getInt("onlineId")
    }


    // Then walk over all matches and update the data to the database
    for {
      matchRaw <- allMatches
      onlineId  = matchRaw.getElementsByTag("matchid").text.toInt
    } {
      if (idsInDb contains onlineId) {
        val isFinished = matchRaw.getElementsByTag("matchisfinished").text.toBoolean
        val (scorea: Int, scoreb: Int) = if (isFinished) {
          val resultScores =  matchRaw.getElementsByTag("matchresult") map {
              m => {
                (m.getElementsByTag("pointsteam1").text.toInt, m.getElementsByTag("pointsteam2").text.toInt)
              }
          }

          resultScores.sortBy({m => m._1 + m._2}).lastOption match {
            case Some(scores) => scores
            case None =>{
              println(s"\tERROR: No valid scores for finished match with id $onlineId")
            }
          }

        } else {
          val result = matchRaw.getElementsByTag("goal").lastOption
          if (result.isDefined) {
            result.get.getElementsByTag("goalscoreteam1").text.toInt
            result.get.getElementsByTag("goalscoreteam2").text.toInt
          } else {
            // This is the case where there are no goals yet
            (-1,-1)
          }
        }
        println(s"\tUpdates match with id $onlineId to $scorea : $scoreb")
        // Finally output the update to the database
        statement.execute(s"UPDATE matches SET " +
          s"scorea='$scorea'," +
          s"scoreb='$scoreb', " +
          s"isfinished='${if(isFinished) 1 else 0}'" +
          s"WHERE onlineid='$onlineId'"
        )

      } else {
        // The match was not in the database, create the entry
        println(s"\t Match with id $onlineId was not yet in the DB, entering it now!")
        val team1 = matchRaw.getElementsByTag("nameteam1").text
        val team2 = matchRaw.getElementsByTag("nameteam2").text
        val groupName = {
          val result = statement.executeQuery(s"select groupchar from team where name='$team1'")
          result.next()
          val c = result.getString("groupchar")
          c
        }
        val isFinished = matchRaw.getElementsByTag("matchisfinished").text.toBoolean
        val (scorea: Int, scoreb: Int) = if (isFinished) {
          val resultScores =  matchRaw.getElementsByTag("matchresult") map {
            m => {
              (m.getElementsByTag("pointsteam1").text.toInt, m.getElementsByTag("pointsteam2").text.toInt)
            }
          }

          resultScores.sortBy({m => m._1 + m._2}).lastOption match {
            case Some(scores) => scores
            case None =>{
              println(s"\tERROR: No valid scores for finished match with id $onlineId")
            }
          }

        } else {
          val result = matchRaw.getElementsByTag("goal").lastOption
          if (result.isDefined) {
            result.get.getElementsByTag("goalscoreteam1").text.toInt
            result.get.getElementsByTag("goalscoreteam2").text.toInt
          } else {
            // This is the case where there are no goals yet
            (-1,-1)
          }
        }
        val date          = sqlDateformat.format(rawDateformat.parse(matchRaw.getElementsByTag("matchdatetimeutc").text))
        val location      = matchRaw.getElementsByTag("locationcity").text
        val stadium       = matchRaw.getElementsByTag("locationstadium").text
        val onlineid      = matchRaw.getElementsByTag("matchid").text.toInt
        val groupId       = matchRaw.getElementsByTag("groupid").text.toInt
        val grouporderid  = matchRaw.getElementsByTag("grouporderid").text.toInt // Indicates which round it is
        val groupname     = matchRaw.getElementsByTag("groupname").text

        statement.execute(s"INSERT INTO matches VALUES " +
          s"(DEFAULT, '$team1', '$team2', '$groupName', '$date', '$location', '$stadium', '$onlineid', " +
          s"'$groupId', '$grouporderid', '$groupname', '${if(isFinished)1 else 0}', '$scorea', '$scoreb')")
        println(s"\tUpdated match $team1 vs. $team2, in group $groupname to the DB with result ${scorea}:${scoreb}")
      }
    }
    val date = sqlDateformat.format(new java.util.Date())
    statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='match'")
    connection.close


//    val conn = DriverManager.getConnection(url, user, pw)
//    val statement = conn.createStatement
//
//    for {
//      m <- Match.unfinishedMatches if(m.date.before(new java.util.Date))
//      id = m.onlineId
//    } {
//
//      println(s"Looking if match ${m.teamA} vs. ${m.teamB} is finished.")
//      val gameraw = Retrieval.getDataOnline(
//        <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
//        <soap12:Body>
//          <GetMatchByMatchID xmlns="http://msiggi.de/Sportsdata/Webservices">
//            <MatchID>{id}</MatchID>
//          </GetMatchByMatchID>
//        </soap12:Body>
//      </soap12:Envelope>)
//
//      // Game is finished, write down the final result to the DB
//      if(gameraw.getElementsByTag("matchisfinished").text.toBoolean) {
//        println(s"Match ${m.teamA} vs. ${m.teamB} is finished! Updating...")
//        // retrieve only the final result
//        val matchresults = gameraw.getElementsByTag("matchresult")
//        val (scoreA, scoreB, isFinished) = matchresults.filter(_.getElementsByTag("resultname").text == "Endergebnis").headOption match {
//          case Some(res) => {
//            (res.getElementsByTag("pointsteam1").text.toInt,
//            res.getElementsByTag("pointsteam2").text.toInt, 1)
//          }
//          case None =>
//            (-1, -1, 0)
//        }
//
//        println(s"updating game ${gameraw.getElementsByTag("nameteam1").text} vs ${gameraw.getElementsByTag("nameteam2").text} to " +
//          s"$scoreA : $scoreB")
//
//        statement.execute(s"UPDATE matches SET isfinished='$isFinished', scorea='$scoreA', scoreb='$scoreB' where onlineid='$id'")
//      } else { // Update the intermediate result so people can see what happens if...
//        val goals = gameraw.getElementsByTag("goal").last
//        val scoreA = goals.getElementsByTag("goalscoreteam1").text.toInt
//        val scoreB = goals.getElementsByTag("goalscoreteam2").text.toInt
//        println(s"updating game ${gameraw.getElementsByTag("nameteam1").text} vs ${gameraw.getElementsByTag("nameteam2").text} to" +
//          s" intermediate result $scoreA : $scoreB")
//        statement.execute(s"UPDATE matches SET scorea='$scoreA', scoreb='$scoreB' where onlineid='$id'")
//      }
//    }
//    val date = sqlDateformat.format(new java.util.Date())
//    statement.execute(s"INSERT into lastupdate value ('match', '$date') ON DUPLICATE KEY UPDATE lastupdate='$date'")
//    conn.close
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

    for (m <- gamesRaw) {
      val team1 = m.getElementsByTag("nameteam1").text
      val team2 = m.getElementsByTag("nameteam2").text
      val groupName = {
        val result = statement.executeQuery(s"select groupchar from team where name='$team1'")
        result.next()
        val c = result.getString("groupchar")
        c
      }
      val isFinished = m.getElementsByTag("matchisfinished").text.toBoolean
      val (score1, score2) = if(isFinished){
        println(s"Match ${team1} vs. ${team2} is finished! Updating...")
        val matchresults = m.getElementsByTag("matchresult")
        val finalResult = matchresults.filter(_.getElementsByTag("resultname").text == "Endergebnis").head
        (finalResult.getElementsByTag("pointsteam1").text.toInt,
        finalResult.getElementsByTag("pointsteam2").text.toInt)
      } else {
        (-1, -1)
      }
      val date          = sqlDateformat.format(rawDateformat.parse(m.getElementsByTag("matchdatetimeutc").text))
      val location      = m.getElementsByTag("locationcity").text
      val stadium       = m.getElementsByTag("locationstadium").text
      val onlineId      = m.getElementsByTag("matchid").text.toInt
      val groupId       = m.getElementsByTag("groupid").text.toInt
      val grouporderid  = m.getElementsByTag("grouporderid").text.toInt
      val groupname     = m.getElementsByTag("groupname").text

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
    statement.execute(s"INSERT into lastupdate value ('match', '$date') ON DUPLICATE KEY UPDATE lastupdate='$date'")
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
    statement.execute(s"INSERT into lastupdate value ('team', '$date') ON DUPLICATE KEY UPDATE lastupdate='$date'")
    conn.close()
  }

  def updateTeams = {
    val conn = DriverManager.getConnection(url, user, pw)
    val statement = conn.createStatement

    val allMatches = Match.allMatches

    var teamMap: Map[String, (Int,Int,Int,Int,Int,Int)] = Map()
    for{
      team <- Team.allTeams
    } teamMap += (team.name -> (0,0,0,0,0,0))

    // Crunch all matches to get the scores for the vorrunde
    for{
      m <- allMatches.filter(ma => ma.isFinished && ma.groupOrderId == 1) //Only use the matches from the vorrunde here
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

    // Finally update the progression of teams in the tournament to the database
    val teamGrouping = allMatches.sortBy(_.groupOrderId)
    for {
      m <- teamGrouping
    } {
      statement.execute(s"UPDATE team SET " +
        s"round='${m.groupOrderId}'" +
        s"WHERE name='${m.teamA}'")
      statement.execute(s"UPDATE team SET " +
        s"round='${m.groupOrderId}'" +
        s"WHERE name='${m.teamB}'")
    }

    val date = sqlDateformat.format(new java.util.Date())
    statement.execute(s"INSERT into lastupdate value ('team', '$date') ON DUPLICATE KEY UPDATE lastupdate='$date'")
    conn.close
  }

  def initializePlayers = {
    val filename    = "src/main/resources/responses.csv"
    val rawData     = scala.io.Source.fromFile(filename).getLines
    val header      = rawData.next.split(',').toList

    val allMatches  = Match.allMatches

    val conn = DriverManager.getConnection(url, user, pw)
    val statement = conn.createStatement

    // parses a line from the file which represents a player
    def parseSingleResponse(response: List[String]): Unit = {

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

      //println(response.drop(5).take(48).zipWithIndex).length()
      // Update the tipps to the current state from the tabular
      try{
        val dbId = statement.executeQuery(s"select id from player where email='$email'")
        dbId.next
        val iid =  dbId.getInt("id")
        // Get all the tipps and save them with their game's online ID (serizalized)
        val tipps = (for{
          (tipp , index) <- response.drop(5).take(48).zipWithIndex
          tipps = tipp.split(':').toList.map(_.toInt)
          teams = header(index + 5).split('-').map(_.trim)
        } yield {
          //assert(teams(0) == allMatches(index).teamA && teams(1) == allMatches(index).teamB)
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
          throw(ex)
        }
      }

      val date = sqlDateformat.format(new java.util.Date())
      //statement.execute(s"UPDATE lastupdate set lastupdate='${date}' where id='player'")
      statement.execute(s"INSERT into lastupdate value ('player', '$date') ON DUPLICATE KEY UPDATE lastupdate='$date'")
    }

    // Iterate over all entries coming from the google-form
    for{
      responseLine <- rawData
      response = responseLine.split(',').toList if(!response.isEmpty && response(0) != "")
    } (parseSingleResponse(response))
    conn.close()
  }

  def updatePlayers = {
    // update the stats to the database
    val conn = DriverManager.getConnection(url, user, pw)
    val statement = conn.createStatement

    // First check if we can find any new tipps for the later rounds in the tournament
    for(round <- 2 to 6) {
      val filename    = s"src/main/resources/responses$round.csv"
      try {

        println(s"\tUpdating the player's tipps for round $round.")
        // TODO mapping from the matches to the online ID's for the tipps
        val rawData     = scala.io.Source.fromFile(filename).getLines
        val header      = rawData.next.split(',').toList.drop(2) // Take only the names of the matches and not the timestamp

        for {
          playerTipp <- rawData
          elems = playerTipp.split(',')
          email = elems(1)
          (tipps, head) <- elems.drop(2) zip header // DO NOT use the timestamp and email
        } {

          val dbId = statement.executeQuery(s"SELECT id from player where email='$email'")
          if(dbId.next) {
            val id = dbId.getInt("id")
            val tippsInDb = statement.executeQuery(s"SELECT tipps1 from player where id='$id'")
            tippsInDb.next
            var currentTipps = tippsInDb.getString("tipps1").unpickle[List[Tipp]]
            val tippedScores = tipps.split(':').toList.map(_.toInt)
            val teams = head.split('-').map(_.trim)
            val teama = teams.head
            val teamb = teams.drop(1).head
            val matchId = statement.executeQuery(s"SELECT onlineid from matches where teama='${teama}' AND teamb='$teamb'")
            matchId.next
            val iid = matchId.getInt("onlineId")
            val newTipp = new Tipp(iid, id, tippedScores(0), tippedScores(1))
            if(! currentTipps.contains(newTipp))
              currentTipps ::= newTipp

            statement.execute(s"UPDATE player SET tipps1='${currentTipps.pickle.value}' where id='$id'")
          } else {
            println(s"Email address $email not in the DB! Could not update tipps.")
          }

        }
      } catch {
        case ex: FileNotFoundException => // nothing to be done here
      }
    }

    // Then calculate the new scores for all players and enter it to the DB
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
    statement.execute(s"INSERT into lastupdate value ('player', '$date') ON DUPLICATE KEY UPDATE lastupdate='$date'")

    conn.close
  }
}
