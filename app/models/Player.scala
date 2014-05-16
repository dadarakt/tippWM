package models

/**
 * Created by jannis on 5/16/14.
 * Models a participant in the game.
 */

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Player(firstName: String, lastName: String, nickName: String, password: String) {
  val id = (firstName + lastName + nickName).hashCode

  override def toString = {
    val nicky = if(nickName.length > 0) s"\'$nickName\'" else ""
    s"$firstName $nicky $lastName"
  }
}

object Player {

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


  def create(id: Int, firstName: String, lastName: String, nickName: String, password: String) {
    DB.withConnection { implicit conn =>
      SQL("insert into player (id, firstName, lastName, nickName, password) values ({id},{firstName},{lastName},{nickName},{password})").on(
        'id -> id,
        'firstName -> firstName,
        'lastName -> lastName,
        'nickName -> nickName,
        'password -> password
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
