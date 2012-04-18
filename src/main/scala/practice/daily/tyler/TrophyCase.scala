package practice.daily.tyler

import com.twitter.logging.{Level,Logger}
import net.liftweb.json._
import scala.collection.JavaConversions._
import scala.collection.immutable._
import scala.collection.mutable.Buffer

class TrophyCase() {

    private val log = Logger.get(getClass)
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    def addReward(id:String, action:String) : Boolean = {

        try {

            val es = new ElasticSearch("tdp-rewards")
            val response = es.index(id = id, estype = "reward", action = action)

        } catch {
            case ex => log.error(ex, "Error adding reward", ex.getMessage)
            return false
        }
            
        return true
    }

    def createIndex() : Boolean = {

        val action = Map("type" -> "string", "index" -> "not_analyzed")
        val timestamp = Map("type" -> "date", "format" -> "date_hour_minute_second") // yyyyMMdd'T'HHmmssZ

        val json = (
            "mappings" -> Map(
                "action" -> Map(
                    "properties" -> Map(
                        "action"   -> action,
                        "timestamp"-> timestamp,
                        "public"   -> Map(
                                "properties" -> Map(
                                    "action"   -> action,
                                    "timestamp"-> timestamp
                                )
                            )
                        )
                    )
                )
            )

        // val response = callES(path = index, method = "POST", content = Some(pretty(render(decompose(json)))))
        // if(response._1 == 200) {
        //     return true
        // }
        false
    }

    def purge(userId : Int) : Boolean = {

        try {

            val es = new ElasticSearch("tdp-rewards")
            val response = es.delete(userId)

        } catch {
            case ex => log.error(ex, "Error adding action", ex.getMessage)
            return false
        }
 
        true
    }
}