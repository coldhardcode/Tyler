package practice.daily.tyler

import com.twitter.logging.{Level,Logger}
import com.twitter.logging.config._

import java.net.{URL,HttpURLConnection}
import java.text.SimpleDateFormat
import java.util.{Date,UUID,TimeZone}
import com.sun.jersey.api.client.{Client,ClientResponse}
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.{read,write}
import scala.collection.JavaConversions._

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
    
    def index(estype : String, action : String) {

        if(!this.verifyIndex) {
            this.createIndex
        }

        val uuid = UUID.randomUUID.toString
        callES(path = "/" + index + "/" + estype + "/" + uuid, method = "PUT", content = Some(action))
    }
    
    def getone(estype : String, id : String) {
        
        val response = callES(path = "/" + index + "/" + estype + "/" + id, method = "GET")
        response._2
    }

    def getActionCountsByDate(id : Int, name : String, days : Int) : scala.collection.mutable.Map[String,BigInt] = {

        val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))
        
        val startDate = "now-" + days.toString + "d"
        
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
                                "gte" -> startDate
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
                                        "gte" -> startDate
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val response = callES(path = "/" + index + "/action/_search", method = "POST", content = Some(compact(render(decompose(json)))))

        val resJson = parse(response._2)

        val actions = resJson \ "facets" \ "actions" \ "entries"
        
        val results = scala.collection.mutable.Map.empty[String,BigInt]

        val foo = actions.values
        foo match {
            case x : List[Map[String,BigInt]] => x foreach {
                entry => {
                    val totalDate = new Date(entry.get("time").get.toLong)
                    results += dateFormatter.format(totalDate.getTime) -> entry.get("count").get
                }
            }
            case Some(x : Any) => // Wtf?
            // case None => // Nothing
        }

        results
    }

    def getActionCounts(id : Int, name : String) : Option[Map[String,BigInt]] = {
        
        val json = Map(
            "query" -> Map(
                "wildcard" -> Map(
                    "action" -> name
                )
            ),
            "filter" -> Map(
                "term" -> Map(
                    "person.id" -> id
                )
            ),
            "facets" -> Map(
                "actions" -> Map(
                    "terms" -> Map(
                        "field" -> "action"
                    ),
                    "facet_filter" -> Map(
                        "term" -> Map(
                            "person.id" -> id
                        )
                    )
                )
            )
        )
        
        val response = callES(path = "/" + index + "/action/_search", method = "POST", content = Some(compact(render(decompose(json)))))

        val resJson = parse(response._2)

        val actions = resJson \ "facets" \ "actions" \ "terms"
        
        val terms = for { JField("term", JString(term)) <- resJson } yield term
        val counts = for { JField("count", JInt(count)) <- resJson } yield count

        Option((terms zip counts) toMap)
    }
  
    def getTimeline(id : Int) : List[Map[String,Any]] = {

        val json = Map(
            "query" -> Map(
                "match_all" -> Map.empty[String,String]
            ),
            "filter" -> Map(
                "term" -> Map(
                    "person.id" -> id
                )
            )
        )
        
        val response = callES(path = "/" + index + "/action/_search", method = "POST", content = Some(compact(render(decompose(json)))))

        val resJson = parse(response._2)
        
        val tl = for { JField("_source", x) <- resJson } yield x
        
        tl.values.asInstanceOf[List[Map[String,Any]]]
    }
  
    def getPublicTimeline(userId : Option[Int], page : Int, count : Int) : List[Map[String,Any]] = {

        val filter = userId match {
            case Some(x : Int) => Map(
                "and" -> List(
                    Map(
                        "exists" -> Map(
                            "field" -> "public.action"
                        )
                    ),
                    Map(
                        "term" -> Map(
                            "person.id" -> userId
                        )
                    )
                )
            )
            case None => Map(
                "exists" -> Map(
                    "field" -> "public.action"
                )
            )
        }

        val from = page match {
            case x if x > 1 => (x - 1) * count
            case _ => 0
        }

        val json = Map(
            "sort" -> List("person.id", "timestamp"),
            "from" -> from,
            "size" -> count,
            "query" -> Map(
                "match_all" -> Map.empty[String,String]
            ),
            "filter" -> filter
        )

        val response = callES(path = "/" + index + "/action/_search", method = "POST", content = Some(compact(render(decompose(json)))))

        val resJson = parse(response._2)

        val publics = resJson \\ "public"
        val tl = for { JField("public", x) <- publics } yield x
        
        tl.values.asInstanceOf[List[Map[String,Any]]]
    }
    
    def delete(id : Int) {
        
        val response = callES(path = "/" + index + "/action/_query?q=person.id:" + id, method = "DELETE")
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
        val timestamp = Map("type" -> "date", "format" -> "date_hour_minute_second") // yyyyMMdd'T'HHmmss

        val json = Map(
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

        val response = callES(path = index, method = "POST", content = Some(pretty(render(decompose(json)))))
        if(response._1 == 200) {
            return true
        }
        false
    }
    
    def callES(path : String, method : String = "GET", content : Option[String] = None) : (Int, String) = {
        
        
        val client = new Client
        val r = client.resource(host + "/" + path);

        log(Level.DEBUG, method + " request to " + host + "/" + path)

        val response : ClientResponse = method match {
            case "GET" => {
                r.get(classOf[ClientResponse])
            }
            case "POST" => {
                content match {
                    case Some(x : String) => r.post(classOf[ClientResponse], x)
                    case None => r.post(classOf[ClientResponse])
                }
            }
            case "DELETE"=> {
                r.delete(classOf[ClientResponse])
            }
            case "HEAD" => {
                r.head
            }
            case "PUT" => {
                content match {
                    case Some(x : String) => r.put(classOf[ClientResponse], x)
                    case None => r.put(classOf[ClientResponse])
                }
            }
        }
        
        val status = response.getStatus
        val resp = response.getEntity(classOf[String])
        
        log(Level.DEBUG, "Response code is " + status)
        
        log(Level.DEBUG, "Response is " + resp)
        
        (status, resp)
    }
}