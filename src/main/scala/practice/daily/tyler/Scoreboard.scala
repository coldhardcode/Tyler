package practice.daily.tyler

import com.twitter.logging.{Level,Logger}
import net.liftweb.json._
import scala.collection.JavaConversions._
import scala.collection.immutable._
import scala.collection.mutable.Buffer

class Scoreboard(val userId:String) {

    private val log = Logger.get(getClass)
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    def getUserKey(key: String): String = {

        return "user/" + userId + "/" + key
    }

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

    def getActionCount(actionName:String) : Option[Map[String,String]] = {
        
         // Fetch all the keys we can find
         // log(Level.DEBUG, "keys " + getUserKey("action-count/" + actionName))
         // val keys = jedis.keys(getUserKey("action-count/" + actionName))

         val es = new ElasticSearch("tdp")
         val response = es.getActionCounts(userId, actionName)

         println(response)

         // Return a 404 since we have nothing to return
         // if(keys.size < 1) {
             // Throw something here? How do we exit?
             return None
         // }

         // Create a new list of keys that is better named by stripping off stuff
         // val newKeys = keys.map(x => {
             // This takes everything to the right of the last /
             // x.takeRight(x.length - x.lastIndexOf("/") - 1)
         // })

         // Fetch the values for our keys
         // toSeq converts our map to a Sequence, I dont' recall what _* is :(
         // val values = jedis.mget(keys.toSeq : _*)

         // Increment the global action count
         // jedis.incr("stats/get/action")

         // Zip together our two Sets into a collection of Tuples then conver it
         // to a map with toMap
         // Option((newKeys zip values) toMap)
    }

    /**
    * Get a user's timeline
    */
    def getTimeline(page : Int = 1, count : Int = 10) : Option[List[JValue]] = {

        val start = (page - 1) * count
        val end = (page * count) - 1

        val es = new ElasticSearch("tdp")
        val response = es.getTimeline(userId)

        val json = parse(response)

        // http://stackoverflow.com/questions/5073747/using-lift-json-is-there-an-easy-way-to-extract-and-traverse-a-list

        val timeline = (json \ "hits" \ "hits").children
        var size = 0
        for (hit <- timeline) size += 1

        log(Level.DEBUG, "Size is " + size)

        // log(Level.DEBUG, "lrange " + getUserKey("timeline") + " " + start + " " + end)
        // Fetch the values for our keys
        // val timeline = jedis.lrange(getUserKey("timeline"), start, end)

        if(size < 1) {
            return None
        }

        // Increment the global action count
        // jedis.incr("stats/get/timeline")

        return Option(timeline)
    }
    
    def purge() : Boolean = {

        try {

            val es = new ElasticSearch("tdp")
            val response = es.delete(userId)

        } catch {
            case ex => log.error(ex, "Error adding action", ex.getMessage)
            return false
        }

        // log(Level.DEBUG, "del " + getUserKey("timeline"))
        // Nix the timeline
        // jedis.del(getUserKey("timeline"))

        // Fetch all the keys we can find
        // log(Level.DEBUG, "keys " + getUserKey("action-count/*"))
        // val keys = jedis.keys(getUserKey("action-count/*"))

        // if(keys.size < 1) {
        //     return false;
        // }

        // Increment the global action count
        // jedis.incr("stats/purge/user")

        // log(Level.DEBUG, "del " + keys)
        // Delete all the keys we got earlier
        // jedis.del(keys.toSeq : _*)

        true
    }
}