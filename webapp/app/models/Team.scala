package models

import java.sql.DriverManager
import models.Database._
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current


/**
 * Created by Jannis on 5/26/14.
 */

case class Team( onlineId: Int,
                 name: String,
                 group: Char,
                 round: Int,
                 iconUrl: String,
                 matchesPlayed: Int,
                 wins: Int,
                 losses: Int,
                 draws: Int,
                 goalsscored: Int,
                 goalsgotten: Int,
                 points: Int){

  override def toString = s"($name, $group)"
}

// TODO retrieval could be done nicer using anorm
object Team {

  val teamParser = {
    get[Int]("onlineId")~
    get[String]("name")~
    get[String]("groupchar")~
    get[Int]("round")~
    get[String]("iconurl")~
    get[Int]("gamesplayed")~
    get[Int]("wins")~
    get[Int]("losses")~
    get[Int]("draws")~
    get[Int]("goalsscored")~
    get[Int]("goalsgotten")~
    get[Int]("points") map {
      case onlineId~name~group~round~iconUrl~gamesPlayed~wins~losses~draws~goalsscored~goalsgotten~points =>
        Team(onlineId, name, group(0), round, iconUrl, gamesPlayed, wins, losses, draws, goalsscored, goalsgotten, points)
    }
  }


  def all: List[Team] =
    DB.withConnection { implicit conn =>
      SQL("select * from team").as(teamParser *)
    }

  // Returns grouped teams, ranked
  def allGroups: List[(Char, List[Team])] = {
    val groupMap = all.groupBy(_.group)
    groupMap.map(t => (t._1, t._2.sortBy(t => (- t.points, - (t.goalsscored - t.goalsgotten))))).toList.sortBy(_._1)
  }
}
