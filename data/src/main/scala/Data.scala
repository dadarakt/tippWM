import java.util.Date

/**
 * Defines the needed datastructures to represent the worlcup
 */

object Data {
  def main(args: Array[String]){
    //getAllGames
    //getAllTeams
    val start = System.currentTimeMillis()
    val aha = Group.allGroups

    println(s"${Match.playedMatches.length}")
    println(s"${Player.allPlayers(0).results}")

    println(s"It took: ${System.currentTimeMillis() - start} ms ")
  }
}

case class Team(name: String, id: Int, group: Char, iconUrl: String){
  override def toString = s"($name, $group)"
}

object Team {
  val allTeams: List[Team] = Retrieval.getAllTeams
}

case class Group(name: Char,
                 teams: Set[Team],
                 games: List[Match]
)

object Group {
  lazy val allGroups: List[Group] = {
    val mapping = Team.allTeams.groupBy(_.group)
    (for{
      groupName <- mapping.keys
    } yield new Group(groupName,
        mapping(groupName).toSet,
        Match.allMatches.filter(_.group == groupName))).toList.sortBy(_.name)
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


case class Player(firstName: String, lastName: String, nickName: String, email: String, tipps: List[Tipp]){
  val id = firstName + lastName + nickName
  lazy val gamesTipped = Match.playedMatches
  val results = Tipp.evaluateTipps(tipps, id)
}

object Player {
  lazy val allPlayers = Retrieval.getAllPlayers

}


















//object Data {

//  val (games, groups) = parseTable("src/main/resources/teams.csv")
//  val responses = parseResponses("src/main/resources/responses.csv")
//
//  def main(args: Array[String]) = {
//    //println(responses)
//
//  }
//
//  def parseResponses(filename: String): List[Player]= {
//    val rawData = scala.io.Source.fromFile(filename).getLines
//    val header = rawData.next
//
//    def parseSingleResponse(response: List[String]): Player = {
//      val firstName = response(1)
//      val lastName  = response(2)
//      val nickName  = response(3)
//      val email     = response(4)
//
//      val first   = response(response.length - 3 )
//      val second  = response(response.length - 2)
//      val third   = response(response.length - 1)
//
//      var index = 0
//      val tipps = for{
//        tipp <- response.drop(5)
//        tipps = tipp.split(':').toList.map(_.toInt)
//      } yield(new Tipp(
//          games(index),
//          tipps(0),
//          tipps(1)
//        ))
//      new Player(firstName, lastName, nickName, email, tipps.toList)
//    }
//
//    (for{
//      responseLine <- rawData
//      response = responseLine.split(',').toList if(!response.isEmpty)
//    } yield(parseSingleResponse(response))).toList
//  }
//
//  def parseTable(filename: String) = {
//    val start = System.currentTimeMillis
//
//    // import the csv file
//    val rawData  = scala.io.Source.fromFile(filename).getLines.toList
//
//    def parseTeams: Set[Team] = {
//      val firstLine = rawData.head.split(',').toList
//      (for{
//        (team, pos) <- firstLine.zipWithIndex
//      } yield new Team(team, ('A' + pos/6).toChar)).toSet
//    }
//
//    val teams = parseTeams
//
//
//
//    def parseGames: List[Game] = {
//      // A mapping for all the timezones
//      val timezones = Map(
//        "Sao Paulo" -> "GMT-03:00",
//        "Natal" -> "GMT-03:00",
//        "Fortaleza" -> "GMT-03:00",
//        "Manaus" -> "GMT-4:00",
//        "Brasilia" -> "GMT-3:00",
//        "Recife" -> "GMT-3:00",
//        "Salvador" -> "GMT-3:00",
//        "Cuiaba" -> "GMT-4:00",
//        "Porto Alegre" -> "GMT-3:00",
//        "Rio de Janeiro" -> "GMT-3:00",
//        "Curitiba" -> "GMT-3:00",
//        "Belo Horizonte" -> "GMT-3:00"
//      )
//
//      // Get all the games in the initial round of the tournament
//      val teamsA = rawData(0).split(',').toList
//      val teamsB = rawData(1).split(',').toList
//      val dates  = rawData(2).split(',').toList
//      val times  = rawData(3).split(',').toList
//      val places = rawData(4).split(',').toList
//
//      val dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy-hh")
//
//      // Create all the games and see which ones were played yet
//      (for {
//        (teamA, pos) <- teamsA.zipWithIndex
//        teamAA  = teams.find(_.name == teamA).get
//        teamB   = teams.find(_.name == teamsB(pos)).get
//        place   = places(pos)
//        date    = {
//          dateFormat.setTimeZone(TimeZone.getTimeZone(timezones(place)))
//          dateFormat.parse(s"${dates(pos)}-${times(pos).split(':').head}")
//        }
//        played  = {
//          date.before(new java.util.Date(System.currentTimeMillis + (1000 * 60 * 60 * 4)))
//        }
//      } yield new Game(teamAA,
//          teamB,
//          teamAA.group,
//          date,
//          place,
//          played)).toList.sortBy(_.date)
//    }
//
//    val games = parseGames
//
//    for {
//      game  <- games if (game.isDone)
//    } (getResult(game))
//
//
//    // TODO make this a little more sophisticated
//    def getResult(game: Game) = {
//      game.scoreA = 100
//      game.scoreB = 100
//    }
//
//    def makeGroups: List[Group] = {
//      val mapping = teams.groupBy(_.group)
//      (for{
//        groupName <- mapping.keys
//      } yield new Group(groupName,
//          mapping(groupName),
//          games.filter(_.group == groupName))).toList.sortBy(_.name)
//    }
//
//    val groups = makeGroups
//    val time = System.currentTimeMillis() - start
//    println(s"<<<<< Run took $time ms >>>>>")
//    (games, makeGroups)
//  }


//}

