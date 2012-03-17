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


class ElasticSearch(val index : String) {

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

        // if(!this.verifyIndex) {
        //     this.createIndex
        // }

        val uuid = UUID.randomUUID.toString
        callES(path = "/" + index + "/actions/" + uuid, method = "PUT", toES = Some(action))
    }
    
    def getone(id : String) {
        
        val url  = new URL(host + "/" + index + "/actions/" + id)
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
        
        val url  = new URL(host + "/" + index + "/actions/_search")
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

        val json = (
            "query" -> (
                ("match_all" -> Map.empty[String,String])
            )
        ) ~
        ("filter" -> (
            "term" -> (
                "user_id" -> id
            )
        ))
        log(Level.DEBUG, pretty(render(json)))
        
        val response = callES(path = "/" + index + "/actions/_search", method = "POST", toES = Some(compact(render(json))))
        log(Level.DEBUG, response._2)
        response._2
      
        // 
        // 
        // val url  = new URL(host + "/" + index + "/actions/_search")
        // val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        // conn.setRequestMethod("GET")
        // conn.setDoOutput(true)
        // conn.setDoInput(true)
        // val writer = new OutputStreamWriter(conn.getOutputStream)
        // writer.write("{" +
        //     "\"query\": {" +
        //         " \"match_all\": {}" +
        //     "}," +
        //     "\"filter\" : {" +
        //         " \"term\" : { \"user_id\" : \"" + id + "\" }" +
        //     "}" +
        // "}")
        // writer.flush
        // 
        // val reader = new BufferedReader(new InputStreamReader(conn.getInputStream))
        // var line = reader.readLine
        // val buffer = new StringBuilder()
        // while((line != null)) {
        //     buffer.append(line)
        //     line = reader.readLine
        // }
        // writer.close
        // reader.close
        // 
        // buffer.toString
    }
    
    def delete(id : String) {
        
        val url  = new URL(host + "/" + index + "/actions/_query?q=user_id:" + id)
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

        log(Level.DEBUG, pretty(render(json)))

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
        
        (conn.getResponseCode, buffer.toString)
    }
}