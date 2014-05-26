import java.sql.DriverManager
import java.util.Date
import Database.{url,user,pw}
import scala.pickling._
import json._

/**
 * Defines the needed datastructures to represent the worlcup
 */

object Data {
  def main(args: Array[String]) = {
    val start = System.currentTimeMillis()
    Group.getGroup('a')
    println(s"${System.currentTimeMillis - start} ms")
  }
    val driver = "com.mysql.jdbc.Driver"
  Class.forName(driver).newInstance

}

case class Player(firstName: String,
                  lastName: String,
                  nickName: String,
                  email: String,
                  tipps: List[Tipp],
                  tippFirst: String,
                  tippSecond: String,
                  tippThird: String){

  val id = firstName + lastName + nickName

  lazy val gamesTipped = Match.playedMatches

  val results = Tipp.evaluateTipps(tipps, id)

  override def toString = {
    s"$firstName '$nickName' $lastName with " +
      s"${results.points} points from ${results.scoredMatches.length} scored matches. (${results.tendencies} tends, " +
      s"${results.diffs} diffs and ${results.hits} hits correct)"
  }
}

object Player {
  def allPlayers: List[Player] ={
    val connection = DriverManager.getConnection(url, user, pw)
    val statement = connection.createStatement
    var players = List[Player]()
    val resultPlayer = statement.executeQuery(s"select * from player")
    while(resultPlayer.next()) {
      val m = new Player(
        resultPlayer.getString("firstname"),
        resultPlayer.getString("lastname"),
        resultPlayer.getString("nickname"),
        resultPlayer.getString("email"),
        resultPlayer.getString("tipps1").unpickle[List[Tipp]],
        resultPlayer.getString("guessfirst"),
        resultPlayer.getString("guesssecond"),
        resultPlayer.getString("guessthird")
      )
      players ::= m
    }
    players
  }
  def rankedPlayers = allPlayers.sortBy(- _.results.points)
}



case class Stats( playerId:             String,
                  scoredMatches:        List[Int], // List of MatchIDs where the player scored
                  falseTippMatches:     List[Int], // List of MatchIDs where the player missed
                  missedMatches:        List[Int], // List of games which weren't tipped for
                  points:               Int,
                  pointsTimeseries:     List[(Date, Int)],
                  tendencies:           Int,
                  tendenciesTimeseries: List[(Date, Int)],
                  diffs:                Int,
                  diffsTimeseries:      List[(Date, Int)],
                  hits:                 Int,
                  hitsTimeseries:       List[(Date, Int)]
                  ) {

  override def toString = {
    s"Player: ${playerId} has ${points} points from ${scoredMatches.length} scored matches. ($tendencies tends, " +
      s" $diffs diffs and $hits hits)"
  }
}


case class Tipp(matchOnlineId: Int, playerId: String, scoreA: Int, scoreB: Int)

object Tipp{

  //returns a time-series of points
  def evaluateTipps(tipps: List[Tipp], playerId: String): Stats = {
    var points      = 0
    var tendencies  = 0
    var diffs       = 0
    var hits        = 0
    var falseTipps  = 0
    var missed      = 0

    var pointsTime      = List[(Date, Int)]()
    var tendenciesTime  = List[(Date, Int)]()
    var diffsTime       = List[(Date, Int)]()
    var hitsTime        = List[(Date, Int)]()
    var missesTime      = List[(Date, Int)]()

    var scoredMatches     = List[Int]()
    var falseTippMatches  = List[Int]()
    var missedMatches     = List[Int]()

    //Iterate over all played matches backwards for efficiency. Will crash on failure, which is ok.
    for {
      m <- Match.playedMatches.reverse
      score = tipps.find(m.onlineId == _.matchOnlineId) match {
        case Some(tipp) => getPointsForTipp(m, tipp)
        case None       => -1 // A game where no tipp exists
      }
    }{
      points += score
      score match {
        case -1 => {
          missed += 1
          missedMatches ::= m.onlineId
        }
        case 0 => {
          falseTipps += 1
          falseTippMatches::= m.onlineId
        }
        case 2 => {
          tendencies += 1
          scoredMatches ::= m.onlineId
        }
        case 3 => {
          diffs += 1
          scoredMatches ::= m.onlineId
        }
        case 4 => {
          hits +=1
          scoredMatches ::= m.onlineId
        }
      }
      pointsTime      ::= (m.date, points)
      tendenciesTime  ::= (m.date, tendencies)
      diffsTime       ::= (m.date, diffs)
      hitsTime        ::= (m.date, hits)
      missesTime      ::= (m.date, falseTipps)
    }

    new Stats(playerId,
      scoredMatches,
      falseTippMatches,
      missedMatches,
      points,
      pointsTime,
      tendencies,
      tendenciesTime,
      diffs,
      diffsTime,
      hits,
      hitsTime
    )
  }

  def getPointsForTipp(m: Match, tipp: Tipp): Int = {
    assert(m.onlineId == tipp.matchOnlineId)

    val diffMatch  = (m.scoreA - m.scoreB)
    val diffTipp   = (tipp.scoreA - tipp.scoreB)

    if (m.scoreA == tipp.scoreA && m.scoreB == tipp.scoreB){
      4
    } else if (diffMatch != 0 && diffMatch == diffTipp) {
      3
    } else if (diffMatch > 0 && diffTipp >0 || diffMatch < 0 && diffTipp < 0 || diffMatch == 0 && diffTipp == 0){
      2
    } else {
      0
    }
  }
}



