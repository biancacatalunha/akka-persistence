package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import part2_event_sourcing.PersistentActors.Accountant.Invoice

import java.util.Date

object PersistentActors extends App {

  /**
   * Scenario: we have a business and an accountant which keeps track of our invoices
   */

  object Accountant {
    // COMMANDS
    case class Invoice(recipient: String, date: Date, amount: Int)
    // EVENTS - data structure that will be persisted
    case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)
  }

  class Accountant extends PersistentActor with ActorLogging {
    import Accountant._

    var latestInvoiceId = 0
    var totalAmount = 0

    //best practice to make it unique
    override def persistenceId: String = "simple-account"

    // normal receive method
    override def receiveCommand: Receive = {
    /**
      * When you receive a command
      * 1) you create an EVENT to persist into the store
      * 2) you persist the event, then pass in a callback that will get triggered once the event is written
      * 3) Update the actor state when the event has persisted
      */
      case Invoice(recipient, date, amount) =>
        log.info(s"Receive invoice for amount: $amount")
        persist(InvoiceRecorded(latestInvoiceId, recipient, date, amount))
        /* time gap: all other messages sent to this actor are stashed */{ e =>
          // It is safe to access mutable state here
          // update state
          latestInvoiceId += 1
          totalAmount += amount

          // correctly identify the sender of the command
//          sender() ! "PersistenceACK"
          log.info(s"Persisted $e as invoice #${e.id}, for total amount $totalAmount")
        }
        // we are not forced to persist events, act like a normal actor
      case "print" =>
        log.info(s"Latest invoice id: $latestInvoiceId, total amount: $totalAmount")
    }

    //Handler that will be called on recovery
    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) =>
        // update state
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Recovered invoice #$id for amount $amount, total amount: $totalAmount")
    }
  }

  val system = ActorSystem("PersistentActor")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

  for(i <- 1 to 10) {
    accountant ! Invoice("The Sofa Company", new Date(), i * 1000)
  }
}
