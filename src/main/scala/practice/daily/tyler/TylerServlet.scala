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
import redis.clients.jedis.exceptions.{JedisConnectionException}
import scala.collection.JavaConversions._
import scala.collection.mutable._

// What the fuck is a case class?
case class Action(name: String)

class TylerServlet extends ScalatraServlet {

    implicit val formats = DefaultFormats
    val jedisPool : JedisPool = new JedisPool("localhost", 6379)
    private val log = Logger.get(getClass)

    val config = new LoggerConfig {
        node = ""
        level = Level.DEBUG
        handlers = new SyslogHandlerConfig {
          // server = "localhost"
        }
    }
    Logger.configure(config)

    get("/") {
        <html>
         <body>
          <h1>Tyler</h1>
         </body>
        </html>
    }

    def getUserKey(userId: String, key: String): String = {

        return "user/" + userId + "/" + key
    }

    /**
    * Create an action
    */
    post("/user/:id/action") {

        val jedis = jedisPool.getResource

        val json = parse(request.body)
        val act = json.extract[Action]

        val board = new Scoreboard(params("id"), jedis)

        val success = board.addAction(act.name, request.body)

        jedisPool.returnResource(jedis)

        if(!success) {
            halt(status = 500)
        }
    }

    /**
    * Get action counts for user
    */
    get("/user/:id/actions") {

        var jedis = jedisPool.getResource
        
        val board = new Scoreboard(params("id"), jedis)

        val actions = board.getActionCount(params.getOrElse("search", "*"))

        jedisPool.returnResource(jedis)

        actions match {
            case Some(list) => {
              // Let JSON-LIFT dump it out for us
              compact(render(decompose(list)))
            }
            case None => {
                log(Level.WARNING, "Attempt get actions for non-existent or inactive user: " + params("id"))
                halt(status = 404)
            }
        }
    }

    /**
    * Get a user's timeline
    */
    get("/user/:id/timeline") {

        val userId = params("id")

        val timeline : Option[Buffer[String]] = try {
            val jedis = jedisPool.getResource

            val page = params.getOrElse("page", "1").toInt
            val count = params.getOrElse("count", "10").toInt

            val start = (page - 1) * count
            val end = (page * count) - 1

            log(Level.DEBUG, "lrange " + getUserKey(userId, "timeline") + " " + start + " " + end)
            // Fetch the values for our keys
            val timeline = jedis.lrange(getUserKey(userId, "timeline"), start, end)

            jedisPool.returnResource(jedis)

            if(timeline.size < 1) {
                None
            } else {
                Option(asScalaBuffer(timeline))
            }
        } catch {
            case e: JedisConnectionException => { log(Level.CRITICAL, "Redis connection failed", e); None }
        }
    
        // Return a 404 since we have nothing to return
        timeline match {
            case Some(tl) => {
                // The timeline is already encoded as JSON so just convert the list into
                // a string with mkString
                "[" + tl.mkString(",") + "]"
            }
            case None => {
                log(Level.WARNING, "Attempt get timeline for non-existent or inactive user: " + userId)
                halt(status = 404)
            }
        }
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
