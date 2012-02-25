package practice.daily.tyler

import java.util.ArrayList
import org.scalatra._
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.{read,write}
import redis.clients.jedis.Jedis
import scala.collection.JavaConversions._

class TylerServlet extends ScalatraServlet {

  implicit val formats = DefaultFormats
  val jedis : Jedis = new Jedis("localhost")

  // What the fuck is a case class?
  case class Action(name: String)

  def getUserKey(userId: String, key: String): String = {
      
      return "user/" + userId + "/" + key
  }

  get("/") {
    <html>
     <body>
      <h1>Tyler</h1>
     </body>
    </html>
  }

  post("/user/:id/action") {
  
    val json = parse(request.body)
    val act = json.extract[Action]

    val userId = params("id")
    val actName = act.name
    
    val trans = jedis.multi
    
    // Increment the count for this event
    trans.incr(getUserKey(userId, actName))
    // Add it to the timeline
    trans.lpush(getUserKey(userId, "timeline"), request.body)
    // Trim the timeline
    trans.ltrim(getUserKey(userId, "timeline"), 0, 99)

    trans.exec
    
    status(200);
  }
  
  get("/user/:id/actions") {
      
    val userId = params("id")

    val search = params.getOrElse("search", "*")

    // Fetch all the keys we can find
    val keys = jedis.keys(getUserKey(userId, "/action-count/" + search))
    
    // Return a 404 since we have nothing to return
    if(keys.size < 1) {
        halt(status = 404)
    }
  
    // Fetch the values for our keys
    val values = jedis.mget(keys.toSeq : _*)

    // Zip together our two Sets into a collection of Tuples then toMap it
    val keysAndValues = (keys zip values) toMap

    // Let JSON-LIFT dump it out for us
    compact(render(decompose(keysAndValues)))
  }

  get("/user/:id/timeline") {
      
    val userId = params("id")

    val page = params.getOrElse("page", "1").toInt
    val count = params.getOrElse("count", "10").toInt

    val start = (page - 1) * count
    val end = (page * count) - 1
    
    println("### LRANGE " + getUserKey(userId, "timeline") + " " + start + " " + end)
    
    // Fetch the values for our keys
    val timeline = jedis.lrange(getUserKey(userId, "timeline"), start, end)

    println(timeline)

    // Return a 404 since we have nothing to return
    if(timeline.size < 1) {
        halt(status = 404)
    }

    // XXX NOT WORKING!
    // Let JSON-LIFT dump it out for us
    compact(render(decompose(timeline)))
  }

  delete("/user/:id") {
      
  }

  notFound {
    status(404)
    <html>
     <body>
      <h1>Tyler has no idea what you mean.</h1>
     </body>
    </html>
  }
}
