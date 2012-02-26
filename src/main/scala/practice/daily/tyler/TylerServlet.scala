package practice.daily.tyler

import com.twitter.logging.{Level, Logger}
import com.twitter.logging.config._
import java.util.ArrayList
import org.scalatra._
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.{read,write}
import redis.clients.jedis.{Jedis,JedisPool}
import scala.collection.JavaConversions._

class TylerServlet extends ScalatraServlet {

  implicit val formats = DefaultFormats
  val jedisPool : JedisPool = new JedisPool("localhost", 6379)
  private val log = Logger.get(getClass)

  // What the fuck is a case class?
  case class Action(name: String)

  val config = new LoggerConfig {
    node = ""
    level = Level.DEBUG
    handlers = new SyslogHandlerConfig {
      // server = "localhost"
    }
  }
  Logger.configure(config)

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

  /**
   * Create an action
   */
  post("/user/:id/action") {
  
    val jedis = jedisPool.getResource

    val json = parse(request.body)
    val act = json.extract[Action]

    val userId = params("id")
    val actName = act.name

    val trans = jedis.multi

    // Increment the count for this event
    log(Level.DEBUG, "incr " + getUserKey(userId, "action-count/" + actName))
    trans.incr(getUserKey(userId, "action-count/" + actName))
    // Add it to the timeline
    log(Level.DEBUG, "lpush " + getUserKey(userId, "timeline"))
    trans.lpush(getUserKey(userId, "timeline"), request.body)
    // Trim the timeline
    trans.ltrim(getUserKey(userId, "timeline"), 0, 99)

    trans.exec
    jedisPool.returnResource(jedis)
    
    status(200);
  }
  
  /**
   * Get action counts for user
   */
  get("/user/:id/actions") {

    val jedis = jedisPool.getResource

    val userId = params("id")

    val search = params.getOrElse("search", "*")

    // Fetch all the keys we can find
    log(Level.DEBUG, "keys " + getUserKey(userId, "action-count/" + search))
    val keys = jedis.keys(getUserKey(userId, "action-count/" + search))
    
    // Return a 404 since we have nothing to return
    if(keys.size < 1) {
      log(Level.WARNING, "Attempt get actions for non-existent or inactive user: " + userId)
      jedisPool.returnResource(jedis)
      halt(status = 404)
    }
    
    // Name the keys something simple for our client
    val newKeys = keys.map(x => {
        x.takeRight(x.length - x.lastIndexOf("/") - 1)
    })
  
    // Fetch the values for our keys
    val values = jedis.mget(keys.toSeq : _*)

    jedisPool.returnResource(jedis)

    // Zip together our two Sets into a collection of Tuples then toMap it
    val keysAndValues = (newKeys zip values) toMap

    status(200);

    // Let JSON-LIFT dump it out for us
    compact(render(decompose(keysAndValues)))
  }

  /**
   * Get a user's timeline
   */
  get("/user/:id/timeline") {

    val jedis = jedisPool.getResource
      
    val userId = params("id")

    val page = params.getOrElse("page", "1").toInt
    val count = params.getOrElse("count", "10").toInt

    val start = (page - 1) * count
    val end = (page * count) - 1
    
    log(Level.DEBUG, "lrange " + getUserKey(userId, "timeline") + " " + start + " " + end)
    // Fetch the values for our keys
    val timeline = jedis.lrange(getUserKey(userId, "timeline"), start, end)

    jedisPool.returnResource(jedis)

    // Return a 404 since we have nothing to return
    if(timeline.size < 1) {
      log(Level.WARNING, "Attempt get timeline for non-existent or inactive user: " + userId)
      halt(status = 404)
    }

    status(200);
    
    // The timeline is already encoded as JSON so just convert the list into
    // a string with mkString
    "[" + asScalaBuffer(timeline).mkString(",") + "]"
  }

  /**
   * Delete all traces of a user
   */
  delete("/user/:id") {

    val jedis = jedisPool.getResource

    val userId = params("id")
      
    log(Level.DEBUG, "del " + getUserKey(userId, "timeline"))
    // Nix the timeline
    jedis.del(getUserKey(userId, "timeline"))
      
    // Fetch all the keys we can find
    log(Level.DEBUG, "keys " + getUserKey(userId, "action-count/*"))
    val keys = jedis.keys(getUserKey(userId, "action-count/*"))

    if(keys.size < 1) {
      log(Level.WARNING, "Attempt to delete non-existent user: " + userId)
      jedisPool.returnResource(jedis)
      // Nothing to delete
      halt(status = 404)
    }

    log(Level.DEBUG, "del " + keys)
    // Delete all the keys we got earlier
    jedis.del(keys.toSeq : _*)
    
    jedisPool.returnResource(jedis)
    
    status(200)
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
