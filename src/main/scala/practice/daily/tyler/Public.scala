package practice.daily.tyler

import com.twitter.logging.{Level,Logger}
import net.liftweb.json._
import scala.collection.JavaConversions._
import scala.collection.immutable._

class Public() {

  private val log = Logger.get(getClass)
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  /**
  * Get the public timeline
  */
  def getTimeline(userId : Option[Int] = None, page : Int = 1, count : Int = 10) : List[Map[String,Any]] = {

    val start = (page - 1) * count
    val end = (page * count) - 1
  
    val es = new ElasticSearch("tdp-actions")
    es.getPublicTimeline(userId, page, count)
  }
}