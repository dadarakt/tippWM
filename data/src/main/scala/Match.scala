import java.sql.DriverManager

/**
 * Created by Jannis on 5/26/14.
 */

case class Match( onlineId : Int,
                  teamA: String,
                  teamB: String,
                  group: Char,
                  date: java.util.Date,
                  location: String,
                  stadium: String,
                  groupId: Int,
                  groupOrderId: Int,
                  groupName: String,
                  isFinished: Boolean = false,
                  scoreA: Int,
                  scoreB: Int) {

  override def toString = s"$teamA vs $teamB on the $date in $location   has result: $scoreA:$scoreB"
}

object Match {
  def allMatches:    List[Match] = {
    val connection = DriverManager.getConnection(Database.url, Database.user, Database.pw)
    val statement = connection.createStatement
    var matches = List[Match]()
    val resultMatch = statement.executeQuery(s"select * from matches")
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
      matches ::= m
    }
    matches.sortBy(_.date)
  }
  def playedMatches: List[Match]      = allMatches.filter(_.isFinished).sortBy(_.date)
  def unfinishedMatches: List[Match]  = allMatches.filter(!_.isFinished).sortBy(_.date)
  def lastMatch                       = playedMatches.view.sortBy(_.date).last

}
