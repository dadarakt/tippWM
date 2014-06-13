package models

/**
 * Created by jannis on 5/16/14.
 * Models a participant in the game.
 */

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import java.util.Date
import java.text.SimpleDateFormat
import scala.Some
import scala.pickling._
import json._

case class Tipp( matchOnlineId: Int,
                 playerId: Int,
                 scoreA: Int,
                 scoreB: Int) {

  def tippString = s"$scoreA : $scoreB"

  def resultString = {
    val m = Match.all.filter(_.onlineId == matchOnlineId).headOption.getOrElse(Match.dummy)

    if (m.isFinished) {
      val diffMatch  = (m.scoreA - m.scoreB)
      val diffTipp   = (scoreA - scoreB)

      if (m.scoreA == scoreA && m.scoreB == scoreB){
        4.toString
      } else if (diffMatch != 0 && diffMatch == diffTipp) {
        3.toString
      } else if (diffMatch > 0 && diffTipp >0 || diffMatch < 0 && diffTipp < 0 || diffMatch == 0 && diffTipp == 0){
        2.toString
      } else {
        0.toString
      }
    } else "--"
  }


}


case class Player(id: Int,
                  firstName: String,
                  lastName: String,
                  nickName: String,
                  email: String,
                  guessFirst: String,
                  guessSecond: String,
                  guessThird: String,
                  tipps1: List[Tipp],
                  scoredMatches: String,
                  falseMatches: String,
                  missedMatches: String,
                  points: Int,
                  pointsTime: String,
                  tendencies: Int,
                  tendenciesTime: String,
                  diffs: Int,
                  diffsTime: String,
                  hits: Int,
                  hitsTime: String
                  ){


  def nameString = {
    val nicky = if(nickName.length > 0) s"\'$nickName\'" else ""
    s"$firstName $nicky $lastName"
  }

  def pointsString = {
    s"$firstName \t $lastName: \t $points, \t(tends: $tendencies, diffs: $diffs, hits: $hits)"
  }

  def nicky = if(nickName.length > 0) s"\'$nickName\'" else ""

  def getTipp(matchId: Int) = {
    val tipp = tipps1.find(_.matchOnlineId == matchId).getOrElse(Tipp(0,0,0,0))
    tipp.tippString
  }

  def getTippPoints(matchId: Int) = {
    val tipp = tipps1.find(_.matchOnlineId == matchId).getOrElse(Tipp(0,0,0,0))
    tipp.resultString
  }
}

object Player {
  def tippy = new Tipp(0,0,0,0)

  val date = {
    get[Option[Date]]("lastupdate") map {
      case Some(date) => {
        date
      }
      case None => {
        val sdf = new SimpleDateFormat("dd/mm/yyy")
        sdf.parse("12/7/1930")
      }
    }
  }

  def lastUpdate = DB.withConnection { implicit conn =>
    SQL("select lastupdate from lastupdate where id='player'").as(date *).headOption.getOrElse(new Date())
  }

  // parser to read a player from the DB
  val player = {
    get[Int]("id")~
    get[String]("firstName")~
    get[String]("lastName")~
    get[String]("nickName")~
    get[String]("email")~
    get[String]("guessfirst")~
    get[String]("guesssecond")~
    get[String]("guessthird")~
    get[Option[String]]("tipps1")~
    get[Option[String]]("scoredMatches")~
    get[Option[String]]("falseMatches")~
    get[Option[String]]("missedMatches")~
    get[Int]("points")~
    get[Option[String]]("pointstime")~
    get[Int]("tendencies")~
    get[Option[String]]("tendenciestime")~
    get[Int]("diffs")~
    get[Option[String]]("diffstime")~
    get[Int]("hits")~
    get[Option[String]]("hitstime") map {
      case  id~firstName~lastName~nickName~email~
            guessfirst~guesssecond~guessthird~tipps1~
            scoredMatches~falseMatches~missedMatches~
            points~pointstime~tendencies~tendenciestime~diffs~diffstime~hits~hitstime => {

      val tipplist = tipps1 match {
          case Some(tipps) => tipps1.get.replaceAll("Tipp", "models.Tipp").unpickle[List[Tipp]]
          case None => List()
        }

        Player(id, firstName, lastName, nickName, email,
          guessfirst, guesssecond, guessthird, tipplist,
          scoredMatches.getOrElse(""),falseMatches.getOrElse(""),missedMatches.getOrElse(""),
          points,pointstime.getOrElse(""),tendencies,tendenciestime.getOrElse(""),diffs,diffstime.getOrElse(""),hits,hitstime.getOrElse(""))
      }
     }
  }

  val simplePlayer = {
    get[Int]("id")~
    get[String]("firstName")~
    get[String]("lastName")~
    get[String]("nickName")~
    get[String]("email") map {
      case id~firstName~lastName~nickName~email =>
        new Player(id,firstName, lastName, nickName, email, "", "","",List(),"","","",0,"",0,"",0,"",0,"")
    }
  }


  def all: List[Player] =
    DB.withConnection { implicit conn =>
      SQL("select * from player").as(player *)
    }



  def getPlayer(id: Int) =
    try {
      DB.withConnection { implicit conn =>
        SQL(s"select * from player where id='${id}'").as(player *).head
      }
    }catch {
      case ex: NoSuchElementException => null
    }


  def ranked = all.sortBy(x => (x.points, x.hits, x.diffs, x.tendencies)).reverse

  def rankedWithTies: List[(Player,Int)] = {
    val sorted = ranked
    var pos = 1
    for{
      (player, i) <- sorted.zipWithIndex
    } yield {
      // If there is still something to compare to
      if(i+1 < sorted.length){
        // Check for total tie
        val r  = (player,pos)
        if(sorted(i+1).points     != sorted(i).points ||
           sorted(i+1).hits       != sorted(i).hits ||
           sorted(i+1).diffs      != sorted(i).diffs ||
           sorted(i+1).tendencies != sorted(i).tendencies){
          pos += 1
        }
        r
      }
      else (player,pos)
    }
  }


  def create(id: Int, firstName: String, lastName: String, nickName: String, mail: String) {
    DB.withConnection { implicit conn =>
      SQL("insert into player (id, firstName, lastName, nickName, mail) values ({id},{firstName},{lastName},{nickName},{mail})").on(
        'id         -> id,
        'firstName  -> firstName,
        'lastName   -> lastName,
        'nickName   -> nickName,
        'mail       -> mail
      ).executeUpdate
    }
  }

  //TODO make sure to use the password here!
  def delete(id: Int) {
    DB.withConnection { implicit c =>
      SQL("delete from player where id = {id}").on(
        'id -> id
      ).executeUpdate
    }
  }


}
