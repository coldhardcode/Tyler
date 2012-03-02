package practice.daily.tyler

import com.twitter.logging.{Level, Logger}
import redis.clients.jedis.Jedis
import scala.collection.JavaConversions._

class Scoreboard(val userId:String, val jedis:Jedis) {

    private val log = Logger.get(getClass)

    def getUserKey(key: String): String = {

        return "user/" + userId + "/" + key
    }

    def addAction(name:String, action:String) : Boolean = {

        try {
            val trans = jedis.multi

            // Increment the count for this event
            log(Level.DEBUG, "incr " + getUserKey("action-count/" + name))
            trans.incr(getUserKey("action-count/" + name))
            // Add it to the timeline
            log(Level.DEBUG, "lpush " + getUserKey("timeline"))
            trans.lpush(getUserKey("timeline"), action)
            // Trim the timeline
            trans.ltrim(getUserKey("timeline"), 0, 99)

            trans.exec
        } catch {
            case ex => log(Level.ERROR, "Error adding action", ex)
            return false
        }
        return true
    }

    def getActionCount(actionName:String) : Option[Map[String,String]] = {
        
         // Fetch all the keys we can find
         log(Level.DEBUG, "keys " + getUserKey("action-count/" + actionName))
         val keys = jedis.keys(getUserKey("action-count/" + actionName))

         // Return a 404 since we have nothing to return
         if(keys.size < 1) {
             // Throw something here? How do we exit?
             return None
         }

         // Create a new list of keys that is better named by stripping off stuff
         val newKeys = keys.map(x => {
             // This takes everything to the right of the last /
             x.takeRight(x.length - x.lastIndexOf("/") - 1)
         })

         // Fetch the values for our keys
         // toSeq converts our map to a Sequence, I dont' recall what _* is :(
         val values = jedis.mget(keys.toSeq : _*)

         // Zip together our two Sets into a collection of Tuples then conver it
         // to a map with toMap
         Option((newKeys zip values) toMap)
    }
}