package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import models.Player

object Application extends Controller {

  val playerForm = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "nickName" -> text,
      "lastName" -> text,
      "password" -> nonEmptyText
    )(Player.apply)(Player.unapply)
  )
  def index = Action {
    Ok(views.html.index("Willkommen zum Tippspiel!"))
  }



  def players = TODO
  def newPlayer = TODO
  def deletePlayer(id: Int) = TODO

}