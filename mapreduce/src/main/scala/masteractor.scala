package mapreduce

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{Broadcast, RoundRobinPool}
import com.typesafe.config.ConfigFactory

class MasterActor extends Actor {
  
  val numberMappers = ConfigFactory.load.getInt("number-mappers")
  val numberReducers = ConfigFactory.load.getInt("number-reducers")
  var pending = numberReducers

  var reduceActors = List[ActorRef]()
  for(i <- 0 until numberReducers)
    reduceActors = context.actorOf(Props[ReduceActor], name="reduce"+i)::reduceActors

  val mapActors = context.actorOf(RoundRobinPool(numberMappers).props(Props(classOf[MapActor], reduceActors)))
  //val mapActors = context.actorOf(Props(new MapActor(reduceActors)).withRouter(RoundRobinPool(numberMappers)))

  def receive = {
    case  MAP(title: String, url: String) =>
      mapActors ! MAP(title, url)
    case Flush =>
      mapActors ! Broadcast(Flush)
    case Done =>
      pending -= 1
      if(pending == 0)
        context.system.shutdown
  }
}
