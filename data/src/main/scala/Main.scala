/**
 * Created by Jannis on 5/22/14.
 */

import akka.actor.ActorSystem
import java.util.concurrent.TimeUnit
import java.util.Date
import scala.concurrent.duration.Duration



object Main {
  def main(args: Array[String]){

    // Things needed to run things periodically
    val actorSystem = ActorSystem()
    val scheduler = actorSystem.scheduler
    implicit val executor = actorSystem.dispatcher

    // Load data, if there is something new
   Database.initializePlayers

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
      interval = Duration(60, TimeUnit.MINUTES),
      runnable = task)
  }

  def updateData = {
    try{
      Database.updateMatches
    } catch {
      case ex: java.net.UnknownHostException => {
        println(s"DID NOT UPDATE MATCH-DATA!! \n The Update will produce stale data since openLiga is no accessible! \n$ex")
      }
    }
    Database.updatePlayers
    Database.updateTeams
  }
}
