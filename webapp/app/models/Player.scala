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

case class Player(firstName: String,
                  lastName: String,
                  nickName: String,
                  email: String,
                  guessFirst: String,
                  guessSecond: String,
                  guessThird: String,
                  tipps1: String,
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
}

object Player {

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
    SQL("select lastupdate from lastupdate where id='player'").as(date *).head
  }

  // parser to read a player from the DB
  val player = {
    get[String]("firstName")~
    get[String]("lastName")~
    get[String]("nickName")~
    get[String]("email")~
    get[String]("guessfirst")~
    get[String]("guesssecond")~
    get[String]("guessthird")~
    get[String]("tipps1")~
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
      case  firstName~lastName~nickName~email~
            guessfirst~guesssecond~guessthird~tipps1~
            scoredMatches~falseMatches~missedMatches~
            points~pointstime~tendencies~tendenciestime~diffs~diffstime~hits~hitstime =>
        Player( firstName, lastName, nickName, email,
                guessfirst, guesssecond, guessthird, tipps1,
                scoredMatches.getOrElse(""),falseMatches.getOrElse(""),missedMatches.getOrElse(""),
                points,pointstime.getOrElse(""),tendencies,tendenciestime.getOrElse(""),diffs,diffstime.getOrElse(""),hits,hitstime.getOrElse(""))
    }
  }

  val simplePlayer = {
    get[String]("firstName")~
    get[String]("lastName")~
    get[String]("nickName")~
    get[String]("email") map {
      case firstName~lastName~nickName~email =>
        new Player(firstName, lastName, nickName, email, "", "","","","","","",0,"",0,"",0,"",0,"")
    }
  }


  def all: List[Player] = DB.withConnection { implicit conn =>
    SQL("select * from player").as(player *)
  }

  def ranked: List[Player] = all.sortBy(_.points)


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
