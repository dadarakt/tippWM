import java.sql.DriverManager

/**
 * Created by Jannis on 5/26/14.
 */

case class Team( onlineId: Int,
                 name: String,
                 group: Char,
                 round: Int,
                 iconUrl: String,
                 gamesplayed: Int,
                 wins: Int,
                 losses: Int,
                 draws: Int,
                 goalsscored: Int,
                 goalsgotten: Int,
                 points: Int){

  override def toString = s"($name, $group)"
}

object Team {
  def main(args: Array[String]) = {
    println(allGroups)
  }
  def allTeams: List[Team] = {

    val connection = DriverManager.getConnection(Database.url, Database.user, Database.pw)
    val statement = connection.createStatement

    var teams = List[Team]()
    val resultTeam = statement.executeQuery(s"select * from team")
    while(resultTeam.next) {
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
      teams ::= t
    }
   teams.sortBy(m=> (m.group))
  }


  // Returns grouped teams, ranked
  def allGroups: List[(Char, List[Team])] = {
    val groupMap = allTeams.groupBy(_.group)
    groupMap.map(t => (t._1, t._2.sortBy(t => (t.points, (t.goalsscored - t.goalsgotten))))).toList
  }
}
