package practice.daily.tyler

import com.twitter.logging.{Level,Logger}
import com.twitter.logging.config._

import java.io.{BufferedReader,FileNotFoundException,InputStreamReader,IOException,OutputStreamWriter}
import java.lang.StringBuilder
import java.net.{URL,HttpURLConnection}
import java.util.UUID
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.{read,write}

// case class Hit(
//     _index : Option[String],
//     _type : Option[String],
//     _id : Option[String],
//     _score : Option[Float],
//     _source : Option[Map[String,Any]]
// )
// 
// case class Hits(
//     total : Int,
//     max_score : Option[Float],
//     hits : Option[List[Hit]]
// )
// 
// case class Result(
//     took : Int,
//     timed_out : Boolean,
//     shards : Option[Map[String,String]],
//     hits : Hits
// )

class ElasticSearch(val index : String) {

    implicit val formats = DefaultFormats // Brings in default date formats etc.
    val log = Logger.get(getClass)
    val host = "http://localhost:9200"

    val config = new LoggerConfig {
        node = ""
        level = Level.DEBUG
        handlers = new ConsoleHandlerConfig {
        // handlers = new SyslogHandlerConfig {
          // server = "localhost"
        }
    }
    Logger.configure(config)
    
    def index(action : String) {

        if(!this.verifyIndex) {
            this.createIndex
        }

        val uuid = UUID.randomUUID.toString
        callES(path = "/" + index + "/actions/" + uuid, method = "PUT", toES = Some(action))
    }
    
    def getone(id : String) {
        
        val response = callES(path = "/" + index + "/actions/" + id, method = "GET")
        response._2
    }

    def getActionCountsByDate(id : Int, name : String) : Option[Map[String,BigInt]] = {
        
        val json = Map(
            "query" -> Map(
                "wildcard" -> Map(
                    "action" -> name
                )
            ),
            "filter" -> Map(
                "and" -> List(
                    Map(
                        "term" -> Map(
                            "person.id" -> id
                        )
                    ),
                    Map(
                        "range" -> Map(
                            "timestamp" -> Map(
                                "gte" -> "2012-03-21T19:01:01"
                            )
                        )
                    )
                )
            ),
            "facets" -> Map(
                "actions" -> Map(
                    "date_histogram" -> Map(
                        "field"     -> "timestamp",
                        "interval"  -> "day"
                    ),
                    "facet_filter" -> Map(
                        "and" -> List(
                            Map(
                                "term" -> Map(
                                    "person.id" -> id
                                )
                            ),
                            Map(
                                "range" -> Map(
                                    "timestamp" -> Map(
                                        "gte" -> "2012-03-21T19:01:01"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        log(Level.DEBUG, pretty(render(decompose(json))))
        
        val response = callES(path = "/" + index + "/actions/_search", method = "POST", toES = Some(compact(render(decompose(json)))))

        val resJson = parse(response._2)

        val actions = resJson \ "facets" \ "actions" \ "entries"
        
        val times = for { JField("time", JString(term)) <- resJson } yield term
        val counts = for { JField("count", JInt(count)) <- resJson } yield count

        Option((times zip counts) toMap)
    }

    def getActionCounts(id : Int, name : String) : Option[Map[String,BigInt]] = {
        
        val json = (
            "query" -> (
                "wildcard" -> (
                    "action" -> name
                )
            )
        ) ~
        (
            "filter" -> (
                "term" -> (
                    "person.id" -> id
                )
            )
        ) ~
        (
            "facets" -> (
                "actions" -> (
                    "terms" -> (
                        "field" -> "action"
                    )
                ) ~
                (
                    "facet_filter" -> (
                        "term" -> (
                            "person.id" -> id
                        )
                    )
                )
            )
        )
        log(Level.DEBUG, pretty(render(json)))
        
        val response = callES(path = "/" + index + "/actions/_search", method = "POST", toES = Some(compact(render(json))))

        val resJson = parse(response._2)

        val actions = resJson \ "facets" \ "actions" \ "terms"
        
        val terms = for { JField("term", JString(term)) <- resJson } yield term
        val counts = for { JField("count", JInt(count)) <- resJson } yield count

        Option((terms zip counts) toMap)
    }
  
    def getTimeline(id : Int) : List[Map[String,Any]] = {

        val json = (
            "query" -> (
                ("match_all" -> Map.empty[String,String])
            )
        ) ~
        ("filter" -> (
            "term" -> (
                "person.id" -> id
            )
        ))
        log(Level.DEBUG, pretty(render(json)))
        
        val response = callES(path = "/" + index + "/actions/_search", method = "POST", toES = Some(compact(render(json))))

        val resJson = parse(response._2)
        
        val tl = for { JField("_source", x) <- resJson } yield x
        
        tl.values.asInstanceOf[List[Map[String,Any]]]
    }
    
    def delete(id : Int) {
        
        val response = callES(path = "/" + index + "/actions/_query?q=person.id:" + id, method = "DELETE")
        response._2
    }

    def deleteIndex() : Boolean = {
        
        log(Level.INFO, "Deleting index")
        val response = callES(path = index, method = "DELETE")
        if(response._1 == 200) {
            return true
        }
        return false
    }
    
    def verifyIndex() : Boolean = {
        
        log(Level.DEBUG, "Checking on index '" + index + "'")
        val response = callES(path = index, method = "HEAD")
        
        log(Level.DEBUG, "Response was " + response._1)
        if(response._1 == 200) {
            return true
        }
        false
    }
    
    def createIndex() : Boolean = {

        val action = Map("type" -> "string", "index" -> "not_analyzed")
        val timestamp = Map("type" -> "date", "format" -> "date_hour_minute_second") // yyyyMMdd'T'HHmmssZ

        val json = (
            "mappings" -> (
                "actions" -> (
                    "properties" -> (
                        ("action"   -> action) ~
                        ("timestamp"-> timestamp) ~
                        ("public"   -> (
                                "properties" -> (
                                    ("action"   -> action) ~
                                    ("timestamp"-> timestamp)
                                )
                            )
                        )
                    )
                )
            )
        )

        val response = callES(path = index, method = "POST", toES = Some(pretty(render(json))))
        if(response._1 == 200) {
            return true
        }
        false
    }
    
    private def callES(path : String, method : String = "GET", toES : Option[String] = None) : (Int, String) = {
        
        val url  = new URL(host + "/" + path)
        log(Level.DEBUG, method + " request to " + url.toString)
        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestMethod(method)
        // Of course we want input
        conn.setDoInput(true)

        toES match {
            case Some(x : String) => {
                conn.setDoOutput(true)
                log(Level.DEBUG, "Sending:")
                log(Level.DEBUG, x)
                val writer = new OutputStreamWriter(conn.getOutputStream)
                writer.write(x)
                writer.flush
                writer.close
            }
            case None => // do nothing
        }

        val stream = try {
            conn.getInputStream
        } catch {
            case x : FileNotFoundException => conn.getErrorStream
            case x : IOException => conn.getErrorStream
        }
        val buffer = new StringBuilder("")

        if(stream != null) {
            val reader = new BufferedReader(new InputStreamReader(stream))
            var line = reader.readLine
            while((line != null)) {
                buffer.append(line)
                line = reader.readLine
            }
            reader.close
        }
        
        log(Level.DEBUG, "Response code is " + conn.getResponseCode)
        
        log(Level.DEBUG, "Response is " + buffer.toString)
        
        (conn.getResponseCode, buffer.toString)
    }
}