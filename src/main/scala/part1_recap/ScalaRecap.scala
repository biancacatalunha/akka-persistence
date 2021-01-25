package part1_recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ScalaRecap extends App {

  val aCondition: Boolean = false
  def myFunction(x: Int) = {
    if(x > 4) 42
    else 65
  }

  //instructions vs expressions
  //type - type inference

  //OO features of Scala
  class Animal
  trait Carnivore { //abstract class
    def eat(a: Animal): Unit
  }
  object Carnivore //companion object

  //generics
  abstract class MyList[+A]

  //method notations
  1 + 2 //infix notation. syntax sugar for 1.+(2)

  //FP
  //Int => Int is syntax sugar for Function1[Int, Int]
  val anIncrementer: Int => Int = (x: Int) => x + 1 // creates an anonymous instance of the function from int to int
  anIncrementer(1)

  //pass a function as an argument. Called higher order functions. e.g. map, flatmap, filter
  List(1,2,3).map(anIncrementer)

  //Monads: Option, Try

  //Pattern matching
  val unknown: Any = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  try {
    throw new RuntimeException
  } catch {
    case e: Exception => println("I caught one")
  }

  /** Advanced Scala */
  //multithreading
  import scala.concurrent.ExecutionContext.Implicits.global

  val future = Future {
    //long computation executed on some other thread
  }
  // map, flatmap, filter + other things e.g recover/recoverWith

  future.onComplete {
    case Success(value) => println(s"I found the meaning of life: $value")
    case Failure(exception) => println(s"I found $exception while searching for the meaning of life")
  }// executed on some thread

  // partial functions are base on pattern matching
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case _ => 999
  }

  // type aliases
  type AkkaReceive = PartialFunction[Any, Unit]
  def receive: AkkaReceive = {
    case 1 => println("Hello")
    case _ => println("Confused")
  }

  //implicits
  implicit val timeout: Int = 20000
  def setTimeout(f: () => Unit)(implicit timeout: Int): Unit = f()
  setTimeout(() => println("timeout")) //other arg list injected by the compiler

  //implicits are often used for conversions
  // 1. implicit methods

  case class Person(name: String) {
    def greet: String = s"Hi my name is $name"
  }

  implicit def fromStringToPerson(name: String) = Person(name)

  "Petter".greet
  // fromStringToPerson("Peter").greet

  // 2. implicit class

  implicit class Dog(name: String) {
    def bark = println("Bark")
  }
  "Lassie".bark
  // new Dog("Lassie").bark

  // implicits organizations
  // 1. local scope
  implicit val numberOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted //the compiler injects the implicit ordering

  // 2. imported scope (see the future example above)

  // 3. Companion object of the types involved in the call
  object Person {
    implicit  val personOrdering: Ordering[Person] = Ordering.fromLessThan((a,b) => a.name.compareTo(b.name) < 0)
  }
  List(Person("Bob"), Person("Alice")).sorted

}
