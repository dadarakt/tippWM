package models

import anorm.SqlParser._
import anorm._
import java.util.Date
import java.text.SimpleDateFormat
import anorm.~
import scala.Some
import play.api.db._
import play.api.Play.current

case class  Match( onlineId : Int,
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

  //override def toString = s"$teamA vs $teamB on the $date in $location   has result: $scoreA:$scoreB"
  val sqlDateformat =  new SimpleDateFormat("dd.MM.yyyy HH:mm")

  def vsString = s"$teamA - $teamB"
  def locationString = s"$location, $stadium"
  def dateString = s"${sqlDateformat.format(date)}"
  def resultString = if(isFinished) s"$scoreA : $scoreB" else "--"
}

object Match {

  def lastUpdate = DB.withConnection { implicit conn =>
    SQL("select lastupdate from lastupdate where id='match'").as(Player.date *).head
  }

  val matchParser = {
      get[String]("teama")~
      get[String]("teamb")~
      get[String]("groupchar")~
      get[Option[Date]]("date") ~
      get[String]("location")~
      get[String]("stadium")~
      get[Int]("onlineId")~
      get[Int]("groupid")~
      get[Int]("grouporderid")~
      get[String]("groupname")~
      get[Boolean]("isfinished")~
      get[Int]("scorea")~
      get[Int]("scoreb") map {
      case  teama~teamb~groupchar~dateOption~location~stadium~onlineid~groupid~grouporderid~groupname~isfinished~scorea~scoreb => {
        val date = dateOption match {
          case Some(date) => {
            date
          }
          case None => {
            val sdf = new SimpleDateFormat("dd/mm/yyy")
            sdf.parse("12/7/1930")
          }
        }
        Match( onlineid,teama,teamb,groupchar(0),date,location,stadium,groupid,grouporderid,groupname,isfinished,scorea,scoreb)
      }
    }
  }

  def all =
    DB.withConnection { implicit conn =>
      SQL("select * from matches").as(matchParser *)
    }

  def dummy = new Match(-1,"dummy", "dummy", 'x', new Date(), "dummy", "dummy", -1, -1, "dummy", false, -1, -1)


  def getMatch(id: Int): Match =
    DB.withConnection { implicit conn =>
      SQL(s"select * from matches where onlineid='$id'").as(matchParser *).headOption.getOrElse(dummy)
    }

  def lastMatches(n: Int) = {
    val matches = all.filter(_.isFinished).sortBy(_.date).reverse
    if(n > matches.length) matches else matches.take(n)
  }

  def lastMatch = lastMatches(1)
}
