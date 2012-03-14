package practice.daily.tyler

import com.twitter.logging.{Level,Logger}

import java.io.{BufferedReader,InputStreamReader,OutputStreamWriter}
import java.lang.StringBuilder
import java.net.{URL,HttpURLConnection}
import java.util.UUID
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.{read,write}


class ElasticSearch() {

    val log = Logger.get(getClass)
    val host = "http://localhost:9200"
    
    def index(action : String) {

        val uuid = UUID.randomUUID.toString
        callES(path = "/tdp/actions/" + uuid, method = "PUT", toES = Some(action))
    }
    
    def getone(id : String) {
        
        val url  = new URL(host + "/tdp/actions/" + id)
        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestMethod("GET")
        conn.setDoOutput(true)
        conn.setDoInput(true)
        
        val reader = new BufferedReader(new InputStreamReader(conn.getInputStream))
        var line = reader.readLine
        val buffer = new StringBuilder()
        while((line != null)) {
            buffer.append(line)
            line = reader.readLine
        }
        reader.close
        
        buffer.toString
    }

    def getActionCounts(id : String, name : String) : String = {
        
        val url  = new URL(host + "/tdp/actions/_search")
        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestMethod("GET")
        conn.setDoOutput(true)
        conn.setDoInput(true)
        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write("{" +
            "\"query\": {" +
                " \"match_all\": {}" +
            "}," +
            "\"filter\" : {" +
                " \"term\" : { \"user_id\" : \"" + id + "\" }" +
            "}," +
            "\"facets\" : {" +
                " \"actions\" : { \"terms\" : { \"field\": \"action\" } }" +
            "}" +
        "}")
        writer.flush
        
        val reader = new BufferedReader(new InputStreamReader(conn.getInputStream))
        var line = reader.readLine
        val buffer = new StringBuilder()
        while((line != null)) {
            buffer.append(line)
            line = reader.readLine
        }
        writer.close
        reader.close
        
        buffer.toString
    }
  
    def getTimeline(id : String) : String = {
        
        val url  = new URL(host + "/tdp/actions/_search")
        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestMethod("GET")
        conn.setDoOutput(true)
        conn.setDoInput(true)
        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write("{" +
            "\"query\": {" +
                " \"match_all\": {}" +
            "}," +
            "\"filter\" : {" +
                " \"term\" : { \"user_id\" : \"" + id + "\" }" +
            "}" +
        "}")
        writer.flush
        
        val reader = new BufferedReader(new InputStreamReader(conn.getInputStream))
        var line = reader.readLine
        val buffer = new StringBuilder()
        while((line != null)) {
            buffer.append(line)
            line = reader.readLine
        }
        writer.close
        reader.close
        
        buffer.toString
    }
    
    def delete(id : String) {
        
        val url  = new URL(host + "/tdp/actions/_query?q=user_id:" + id)
        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestMethod("DELETE")
        conn.setDoInput(true)
        
        val reader = new BufferedReader(new InputStreamReader(conn.getInputStream))
        var line = reader.readLine
        val buffer = new StringBuilder()
        while((line != null)) {
            buffer.append(line)
            line = reader.readLine
        }
        reader.close
        
        buffer.toString
    }
    
    def createIndex() {

        val action = Map("type" -> "string", "index" -> "not_analyzed")
        val timestamp = Map("type" -> "date", "format" -> "basic_date_time_no_millis") // yyyyMMdd'T'HHmmssZ

        val json = (
            "mappings" -> (
                "action" -> (
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

        // println(pretty(render(json)))

        callES(path = "foobar", method = "POST", toES = Some(pretty(render(json))))
    }
    
    private def callES(path : String, method : String = "GET", toES : Option[String] = None) : String = {
        
        val url  = new URL(host + "/" + path)
        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestMethod(method)
        // Of course we want input
        conn.setDoInput(true)

        toES match {
            case Some(x : String) => {
                conn.setDoOutput(true)
                val writer = new OutputStreamWriter(conn.getOutputStream)
                writer.write(x)
                writer.flush
                writer.close
            }
            case None => // do nothing
        }

        val reader = new BufferedReader(new InputStreamReader(conn.getInputStream))
        var line = reader.readLine
        val buffer = new StringBuilder()
        while((line != null)) {
            buffer.append(line)
            line = reader.readLine
        }
        reader.close
        
        buffer.toString
    }
}