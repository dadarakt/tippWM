package models

/**
 * Created by jannis on 5/16/14.
 * Models a participant in the game.
 */

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Player(firstName: String,
                  lastName: String,
                  nickName: String,
                  email: String,
                  tipps: String,
                  tippFirst: String,
                  tippSecond: String,
                  tippThird: String){

  override def toString = {
    val nicky = if(nickName.length > 0) s"\'$nickName\'" else ""
    s"$firstName $nicky $lastName"
  }
}

object Player {

  // parser to read a player from the DB
  val player = {
    get[String]("firstName")~
    get[String]("lastName")~
    get[String]("nickName")~
    get[String]("email")~
    get[String]("tipps1")~
    get[String]("guessfirst")~
    get[String]("guesssecond")~
    get[String]("guessthird") map {
      case firstName~lastName~nickName~email~tipps1~guessfirst~guesssecond~guessthird =>
        Player(firstName, lastName, nickName, email, tipps1,guessfirst, guesssecond, guessthird)
    }
  }

  val simplePlayer = {
    get[String]("firstName")~
    get[String]("lastName")~
    get[String]("nickName") map {
      case firstName~lastName~nickName =>
        Player(firstName, lastName, nickName, "", "", "","","")
    }
  }


  def all: List[Player] = DB.withConnection { implicit conn =>
    SQL("select * from player").as(player *)
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
