/**
 * Created by Jannis on 5/22/14.
 */

import akka.actor.ActorSystem
import Database.{url,user,pw}
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import java.util.Date
import scala.concurrent.duration.Duration
import scala.pickling._
import json._

object Main {
  def main(args: Array[String]){

    // Things needed to run things periodically
    val actorSystem = ActorSystem()
    val scheduler = actorSystem.scheduler
    implicit val executor = actorSystem.dispatcher

    // The task which is performed to generate the data
    val task = new Runnable {
      def run(){
        println(s"${new Date()}: Updating data.")
        val start = System.currentTimeMillis
        updateData
        println(s"Update succesful. (${System.currentTimeMillis - start} ms)")
      }
    }
    scheduler.schedule(
      initialDelay = Duration(1, TimeUnit.SECONDS),
      interval = Duration(10, TimeUnit.SECONDS),
      runnable = task)

  }

  def updateData = {
    // TODO update the matches before continuing

    // All matches which are played
    val playedMatches = Match.playedMatches

    // update the stats to the database
    val connection = DriverManager.getConnection(url, user, pw)
    val statement = connection.createStatement
    for {
      player <- Player.allPlayers
      stats = player.results
    } {
      statement.execute(s"UPDATE player SET " +
        s"scoredMatches='${stats.scoredMatches.pickle.value}'," +
        s"falseMatches='${stats.falseTippMatches.pickle.value}'," +
        s"missedMatches='${stats.missedMatches.pickle.value}'," +
        s"points='${stats.points}'," +
        s"pointstime='${stats.pointsTimeseries.pickle.value}'," +
        s"tendencies='${stats.tendencies}'," +
        s"tendenciestime='${stats.tendenciesTimeseries.pickle.value}'," +
        s"diffs='${stats.diffs}'," +
        s"diffstime='${stats.diffsTimeseries.pickle.value}'," +
        s"hits='${stats.hits}'," +
        s"hitstime='${stats.hitsTimeseries.pickle.value}' " +
        s"WHERE email='${player.email}'")
    }

    connection.close()

  }
}
