import java.util.Date

/**
 * Defines the needed datastructures to represent the worlcup
 */

object Data {

}

class State {

}

case class Team(name: String, id: Int, group: Char, iconUrl: String){
  override def toString = s"($name, $group)"
}

object Team {
  val allTeams: List[Team] = Retrieval.getAllTeams
}

case class Group(name: Char,
                 teams: Set[Team],
                 matches: List[Match]){

  // name, gamesplayed, scored, gotten, diff, points
  val ranking: List[(String,Int,Int,Int,Int,Int)] =
  {
    // The maps to keep the scores
    var points      = Map[String, Int]()
    var goalsScored = Map[String, Int]()
    var goalsGotten = Map[String, Int]()

    for(team <- teams){
      points += (team.name -> 0)
      goalsScored += (team.name -> 0)
      goalsGotten += (team.name -> 0)
    }

    for {
      m <- matches.filter(_.isFinished)
      (pointsA, pointsB) = if(m.scoreA > m.scoreB) (3,0) else if (m.scoreA == m.scoreB) (1,1) else (0,3)
    }{
      points      ++= Map((m.teamA -> (points(m.teamA) + pointsA)),(m.teamB -> (points(m.teamB) + pointsB)))
      goalsScored ++= Map((m.teamA -> (goalsScored(m.teamA) + m.scoreA)), (m.teamB -> (goalsScored(m.teamB) + m.scoreB)))
      goalsGotten ++= Map((m.teamA -> (goalsGotten(m.teamA) + m.scoreB)), (m.teamB -> (goalsGotten(m.teamB) + m.scoreA)))
    }

    (points.map{ t =>
      val name = t._1
      (name, matches.filter(m => m.teamA == name || m.teamB == name).length,
        goalsScored(name), goalsGotten(name), goalsScored(name) - goalsGotten(name), points(name))
    }).toList.sortBy(- _._6)

  }

  override def toString = {
    ranking.mkString("\n")
  }

}

object Group {
  val allGroups: List[Group] = {
    val mapping = Team.allTeams.groupBy(_.group)
    (for{
      groupName <- mapping.keys
    } yield new Group(groupName,
        mapping(groupName).toSet,
        Match.allMatches.filter(_.group == groupName))).toList.sortBy(_.name)
  }
  assert(allGroups.forall(_.teams.size == 4))
  assert(allGroups.forall(_.matches.size == 6))

  def allGroupsString = {
    allGroups.mkString("\n--------------------------------------------\n")
  }

}


case class Match(teamA: String,
                teamAId: Int,
                teamB: String,
                teamBId: Int,
                group: Char,
                date: java.util.Date,
                location: String,
                locationId: Int,
                stadium: String,
                id : Int,
                groupId: Int,
                groupOrderId: Int,
                groupName: String,
                scoreA: Int,
                scoreB: Int,
                isFinished: Boolean = false) {

  //override def toString = s"$teamA vs $teamB on the $date at $location   has result: $scoreA:$scoreB"
}

object Match {
  lazy val allMatches: List[Match] = Retrieval.getAllGamesVorrunde.sortBy(_.group)
  lazy val playedMatches: List[Match] = allMatches.filter(_.isFinished).sortBy(_.date)
  def lastMatch = playedMatches.view.sortBy(_.date).last
}

case class Tipp(matchId: Int, playerId: String, scoreA: Int, scoreB: Int)

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
      score = tipps.find(m.id == _.matchId) match {
        case Some(tipp) => getPointsForTipp(m, tipp)
        case None       => -1 // A game where no tipp exists
      }
    }{
      points += score
      score match {
        case -1 => {
          missed += 1
          missedMatches ::= m.id
        }
        case 0 => {
          falseTipps += 1
          falseTippMatches::= m.id
        }
        case 2 => {
          tendencies += 1
          scoredMatches ::= m.id
        }
        case 3 => {
          diffs += 1
          scoredMatches ::= m.id
        }
        case 4 => {
          hits +=1
          scoredMatches ::= m.id
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
    assert(m.id == tipp.matchId)

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


case class Player(firstName: String,
                  lastName: String,
                  nickName: String,
                  email: String,
                  tipps: List[Tipp],
                  tippFirst: Team,
                  tippSecond: Team,
                  tippThird: Team){

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
  lazy val allPlayers = Retrieval.getAllPlayers
  lazy val rankedPlayers = allPlayers.sortBy(- _.results.points)
}



