package practice.daily.tyler

import com.twitter.logging.{Level,Logger}
import net.liftweb.json._
import scala.collection.JavaConversions._
import scala.collection.immutable._
import scala.collection.mutable.Buffer

class Scoreboard() {

    private val log = Logger.get(getClass)
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    def addAction(action:String) : Boolean = {

        try {

            val es = new ElasticSearch("tdp")
            val response = es.index(action)

        } catch {
            case ex => log.error(ex, "Error adding action", ex.getMessage)
            return false
        }
        return true
    }

    def getActionCount(userId : Int, actionName : String) : Option[Map[String,BigInt]] = {
        
         val es = new ElasticSearch("tdp")
         val actions = es.getActionCounts(userId, actionName)

         // Return a 404 since we have nothing to return
         if(actions.size < 1) {
             // Throw something here? How do we exit?
             return None
         }

         return actions
    }

    /**
    * Get a user's timeline
    */
    def getTimeline(userId : Int, page : Int = 1, count : Int = 10) : Option[List[Map[String,Any]]] = {

        val start = (page - 1) * count
        val end = (page * count) - 1

        val es = new ElasticSearch("tdp")
        val timeline = es.getTimeline(userId)

        if(timeline.size < 1) {
            return None
        }

        return Option(timeline)
    }
    
    def purge(userId : Int) : Boolean = {

        try {

            val es = new ElasticSearch("tdp")
            val response = es.delete(userId)

        } catch {
            case ex => log.error(ex, "Error adding action", ex.getMessage)
            return false
        }
 
        true
    }
}