package practice.daily.tyler

import com.twitter.logging.{Level,Logger}

import java.io.{BufferedReader,InputStreamReader,OutputStreamWriter}
import java.lang.StringBuilder
import java.net.{URL,HttpURLConnection}
import java.util.UUID

class ElasticSearch() {

    val log = Logger.get(getClass)
    val host = "http://localhost:9200"
    
    def index(action : String) {

        val uuid = UUID.randomUUID.toString
        val url  = new URL(host + "/tdp/actions/" + uuid)
        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestMethod("PUT")
        conn.setDoOutput(true)
        conn.setDoInput(true)
        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write(action)
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
}