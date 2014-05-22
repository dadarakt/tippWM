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
                  password: String,
                  mail: String = ""){

  val id:  Int = (firstName + lastName + nickName).hashCode

  override def toString = {
    val nicky = if(nickName.length > 0) s"\'$nickName\'" else ""
    s"$firstName $nicky $lastName"
  }
}

object Player {
  def apply(firstName:String, lastName:String, nickName:String, password:String) =
    new Player(firstName, lastName, nickName, password)
  val player = {
    get[String]("firstName")~
    get[String]("lastName")~
    get[String]("nickName")~
    get[String]("password") map {
      case firstName~lastName~nickName~password => Player(firstName, lastName, nickName, password)
    }
  }


  def all: List[Player] = DB.withConnection { implicit conn =>
    SQL("select * from player").as(player *)
  }


  def create(id: Int, firstName: String, lastName: String, nickName: String, mail: String, password: String) {
    DB.withConnection { implicit conn =>
      SQL("insert into player (id, firstName, lastName, nickName, mail, password) values ({id},{firstName},{lastName},{nickName},{mail},{password})").on(
        'id         -> id,
        'firstName  -> firstName,
        'lastName   -> lastName,
        'nickName   -> nickName,
        'mail       -> mail,
        'password   -> password
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
