package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import models.Player

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Willkommen zum Tippspiel!"))
  }

//  val playerForm = Form(
//    mapping(
//      "firstName" -> nonEmptyText,
//      "lastName" -> text,
//      "nickName" -> text,
//      "mail"     -> email
//    )(Player.apply)(Player.unapply)
//  )

//  val deleteForm = Form(
//    "password" -> nonEmptyText
//  )

  def players = Action(
    Ok(views.html.spieler(Player.all))
  )

  def anmeldung = Action(
    Ok(views.html.anmeldung("Willkommen!"))
  )

  def punkte = Action(
    Ok(views.html.punkte(Player.all))
  )


//  def newPlayer = Action { implicit request =>
//    playerForm.bindFromRequest.fold(
//      errors => BadRequest(views.html.spieler(Player.all, errors)),
//      player => {
//        Player.create(player.id, player.firstName, player.lastName, player.nickName, player.mail)
//        Redirect(routes.Application.players)
//      }
//    )
//  }

  def deletePlayer(id: Int) = Action {
    Player.delete(id)
    Redirect(routes.Application.players)
  }

}