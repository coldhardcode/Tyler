package practice.daily.tyler

import com.twitter.logging.{Level, Logger}
import com.twitter.logging.config._
import java.util.ArrayList
import org.scalatra._
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.{read,write}
import scala.collection.JavaConversions._
import scala.collection.mutable._

class TylerServlet extends ScalatraServlet {

    implicit val formats = DefaultFormats
    private val log = Logger.get(getClass)

    val config = new LoggerConfig {
        node = ""
        level = Level.DEBUG
        handlers = new ConsoleHandlerConfig {
        // handlers = new SyslogHandlerConfig {
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
    * Create an action XXX Should this be a PUT?
    */
    post("/user/:id/action") {

        val board = new Scoreboard(userId = params("id"))
        val success = board.addAction(action = request.body)

        if(!success) {
            halt(status = 500)
        }
    }

    /**
    * Get action counts for user
    */
    get("/user/:id/actions") {

        val board = new Scoreboard(userId = params("id"))
        val actions = board.getActionCount(actionName = params.getOrElse("search", "*"))

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

        val board = new Scoreboard(userId = params("id"))
        val timeline = board.getTimeline(page = params.getOrElse("page", "1").toInt, count = params.getOrElse("count", "10").toInt)
    
        // Return a 404 since we have nothing to return
        timeline match {
            case Some(tl) => {
                // The timeline is already encoded as JSON so just convert the list into
                // a string with mkString
                compact(render(decompose(tl)))
            }
            case None => {
                log(Level.WARNING, "Attempt get timeline for non-existent or inactive user: " + params("id"))
                halt(status = 404)
            }
        }
    }

    /**
    * Delete all traces of a user
    */
    delete("/user/:id") {

        val board = new Scoreboard(userId = params("id"))
        val success = board.purge

        if(!success) {
            log(Level.WARNING, "Attempt to delete non-existent user: " + params("id"))
            // Nothing to delete
            halt(status = 404)
        }
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
