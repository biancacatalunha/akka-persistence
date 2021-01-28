package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.persistence.PersistentActor
import part2_event_sourcing.PersistentActors.Accountant.{Invoice, Shutdown}

import java.util.Date

/**
  * The actor receives commands and persists events
  * It can persist one event persist(event) or many persistAll(List[Event])
  *
  * receiveRecover will restore and replay all events for an actor in order
  *
  * Persist Failure:
  * onPersistFailure - if sending the event to the journal fails, the actor is stopped
  * Best practice: start the actor again after a while. (use backoff supervisor)
  *
  * onPersistRejected - if the journal fails to write an event, the actor is resumed
  *
  * Shutdown of persistent actors
  * PoisonPill is sent to a different mailbox
  * In the case bellow, it is executed before the events are persisted
  * so the events are sent to dead letters
  *
  * Best practice: Define you own shutdown
  * 1. Define a Shutdown case object
  * 2. Handle the Shutdown command in the actor's receive method
  * 3. Stop the actor context.stop(self)
  * By doing that, the message will be put in the same mailbox so the shutdown
  * will be handled after the messages are persisted
  *
  * Never call persist or persistAll from futures!! Risk of actor state corruption
  */

object PersistentActors extends App {

  /**
   * Scenario: we have a business and an accountant which keeps track of our invoices
   */

  object Accountant {
    // COMMANDS
    case class Invoice(recipient: String, date: Date, amount: Int)
    case class InvoiceBulk(invoices: List[Invoice])
    // SPECIAL COMMANDS
    case object Shutdown
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

      // Persist multiple events
      case InvoiceBulk(invoices) =>
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        // The zip method takes another collection as parameter and will merge its elements with the elements of the
        // current collection to create a new collection consisting of pairs or Tuple2 elements from both collections.
        val events = invoices.zip(invoiceIds).map {pair =>
          val id = pair._2
          val invoice = pair._1

          InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }
        persistAll(events) {e =>
          latestInvoiceId += 1
          totalAmount += e.amount

          log.info(s"Persisted single $e as invoice #${e.id}, for total amount $totalAmount")
        }

      case Shutdown => context.stop(self)
    }

    /** This method is called if persisting failed, the actor will be STOPPED
        Best practice: start the actor again after a while. (use backoff supervisor)*/
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persist $event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    /** Called if the JOURNAL fails to persist the event
    The actor is RESUMED */
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persist rejected for $event because of $cause")
      super.onPersistRejected(cause, event, seqNr)
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
//  accountant ! PoisonPill
  accountant ! Shutdown

  val newInvoices = for (i <- 1 to 5) yield Invoice("The awesome chairs", new Date, i * 2000)
//  accountant ! InvoiceBulk(newInvoices.toList)

}
