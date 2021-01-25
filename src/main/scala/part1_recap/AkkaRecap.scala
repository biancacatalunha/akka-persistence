package part1_recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props, Stash, SupervisorStrategy}
import akka.util.Timeout

object AkkaRecap extends App {

  class SimpleActor extends Actor with ActorLogging with Stash {
    override def receive: Receive = {
      case "createChild" =>
        val childActor = context.actorOf(Props[SimpleActor], "myChild")
      case "stashThis" =>
        stash()
        context.become(anotherHandler())
      case "change" => context.become(anotherHandler())
      case message => log.info(s"I received: $message")
    }

    def anotherHandler(): Receive = {
      case message => log.info("another handler")
    }

    override def preStart(): Unit = {
      log.info("I am starting")
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _:RuntimeException => Restart
      case _ => Stop
    }
  }

  val system = ActorSystem("AkkaRecap")
  // you can only instantiate an actor through the actor system
  val actor =  system.actorOf(Props[SimpleActor])
  // you can only communicate to an actor by sending it a message
  actor ! "hello"

  /**
    * messages are sent asynchronously
    * many actors (in the millions) can share a few dozen threads
    * each message is processed/handled ATOMICALLY
    * no need for locking
    * Changing actor behaviour + stashing
    * actors can spawn other actors
    * guardians: /system, /user, / = root
    *
    * Actors have a defined lifecycle:
    * start, stop, resume, suspend, restart
    *
    * Logging (with ActorLogging)
    *
    * Supervision decides how parent actors are going to respond to child actor's failure
    *
    * Infrastructure configuration: Dispatchers, Routers, Mailboxes
    *
    * Schedulers
    *
    * Akka patterns including FSM + ask pattern
    * Common practice to use the ask pattern with pipe
    */
  import system.dispatcher

  import scala.concurrent.duration._

  system.scheduler.scheduleOnce(2 seconds) {
    actor ! "Delayed Happy Birthday"
  }

  import akka.pattern.ask
  implicit val timeout: Timeout = Timeout(3 seconds)

  val future = actor ? "question"

  // the pipe pattern
  import akka.pattern.pipe
  val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")
  future.mapTo[String].pipeTo(anotherActor) // when the future is completed the message is sent to another actor
}
