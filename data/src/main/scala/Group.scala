import Database.{url,user,pw}
import java.sql.DriverManager
import scala.util.control.NonFatal

/**
 * Created by Jannis on 5/26/14.
 */


case class Group(name: Char,
                 teams: Set[Team],
                 matches: List[Match]){

  // name, gamesplayed, scored, gotten, diff, points
  //  val ranking: List[(String,Int,Int,Int,Int,Int)] =
  //  {
  //    // The maps to keep the scores
  //    var points      = Map[String, Int]()
  //    var goalsScored = Map[String, Int]()
  //    var goalsGotten = Map[String, Int]()
  //
  //    for(team <- teams){
  //      points      += (team.name -> 0)
  //      goalsScored += (team.name -> 0)
  //      goalsGotten += (team.name -> 0)
  //    }
  //
  //    for {
  //      m <- matches.filter(_.isFinished)
  //      (pointsA, pointsB) = if(m.scoreA > m.scoreB) (3,0) else if (m.scoreA == m.scoreB) (1,1) else (0,3)
  //    }{
  //      points      ++= Map((m.teamA -> (points(m.teamA) + pointsA)),(m.teamB -> (points(m.teamB) + pointsB)))
  //      goalsScored ++= Map((m.teamA -> (goalsScored(m.teamA) + m.scoreA)), (m.teamB -> (goalsScored(m.teamB) + m.scoreB)))
  //      goalsGotten ++= Map((m.teamA -> (goalsGotten(m.teamA) + m.scoreB)), (m.teamB -> (goalsGotten(m.teamB) + m.scoreA)))
  //    }
  //
  //    (points.map{ t =>
  //      val name = t._1
  //      (name, matches.filter(m => m.teamA == name || m.teamB == name).length,
  //        goalsScored(name), goalsGotten(name), goalsScored(name) - goalsGotten(name), points(name))
  //    }).toList.sortBy(- _._6)
  //
  //  }

  //  override def toString = {
  //    ranking.mkString("\n")
  //  }

}

object Group {

  /**
   * Loads a group from the database
   * @param name The name of the group to return
   * @return
   */
  def getGroup(name: Char): Group = {

    val conn = DriverManager.getConnection(url, user, pw)

    try {
      val statement = conn.createStatement()

      var teams = Set[Team]()
      val resultTeam = statement.executeQuery(s"select * from team where groupchar='$name'")
      while (resultTeam.next) {

        val t = new Team(resultTeam.getInt("onlineid"),
          resultTeam.getString("name"),
          resultTeam.getString("groupchar")(0),
          resultTeam.getInt("round"),
          resultTeam.getString("iconurl"),
          resultTeam.getInt("gamesplayed"),
          resultTeam.getInt("wins"),
          resultTeam.getInt("losses"),
          resultTeam.getInt("draws"),
          resultTeam.getInt("goalsscored"),
          resultTeam.getInt("goalsgotten"),
          resultTeam.getInt("points"))
        teams += t
      }

      var matches = List[Match]()
      val resultMatch = statement.executeQuery(s"select * from matches where groupchar='$name'")
      while(resultMatch.next()) {
        val m = new Match(resultMatch.getInt("onlineid"),
          resultMatch.getString("teama"),
          resultMatch.getString("teamb"),
          resultMatch.getString("groupchar")(0),
          resultMatch.getDate("date"),
          resultMatch.getString("location"),
          resultMatch.getString("stadium"),
          resultMatch.getInt("groupid"),
          resultMatch.getInt("grouporderid"),
          resultMatch.getString("groupname"),
          resultMatch.getBoolean("isfinished"),
          resultMatch.getInt("scorea"),
          resultMatch.getInt("scoreb"))
        println(m)
        matches ::= m
      }
      new Group(name, teams, matches.sortBy(_.date))

    } catch {
      case ex: scala.pickling.PicklingException => {
        println(s"Error while deserialization, $ex")
        new Group(name, Set(), List())
      }
      case NonFatal(ex) => {
        println(s"Could not connect to DB, $ex")
        new Group(name, Set(), List())
      }

    } finally {
      conn.close
    }
  }


  def allGroups: List[Group] = {
    val mapping = Team.allTeams.groupBy(_.group)
    val groups = (for{
      groupName <- mapping.keys
    } yield new Group(groupName,
        mapping(groupName).toSet,
        Match.allMatches.filter(_.group == groupName))).toList.sortBy(_.name)
    assert(allGroups.forall(_.teams.size == 4))
    assert(allGroups.forall(_.matches.size == 6))
    groups
  }


  def allGroupsString = {
    allGroups.mkString("\n--------------------------------------------\n")
  }

}
