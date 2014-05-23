/**
 * Created by Jannis on 5/22/14.
 */

import akka.actor.ActorSystem
import Data._
import java.util.concurrent.TimeUnit
import java.util.Date
import scala.concurrent.duration.Duration

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
      interval = Duration(10, TimeUnit.MINUTES),
      runnable = task)

    Thread.sleep(20000)

    actorSystem.shutdown()
  }

  def updateData = {


    val aha = Group.allGroups

    println(s"${Match.playedMatches.length}")
    println(s"${Player.allPlayers(0).results}")
    println(Player.rankedPlayers)
    println(Group.allGroupsString)
  }
}
