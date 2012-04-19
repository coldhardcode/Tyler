package practice.daily.tyler

import com.twitter.logging.{Level,Logger}
import net.liftweb.json._
import scala.collection.JavaConversions._
import scala.collection.immutable._

class Public() {

  private val log = Logger.get(getClass)
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  def getUserTimeline(userId : Int, page : Int = 1, count : Int = 25) : List[Map[String,Any]] = {

    val es = new ElasticSearch("tdp-actions")
    es.getPublicTimeline(Some(userId), page, count)
  }

  /**
  * Get the public timeline
  */
  def getTimeline(page : Int = 1, count : Int = 25) : Map[String,Any] = {

    val es = new ElasticSearch("tdp-actions")
    val tl = es.getPublicTimeline(None, page, count)

    tl.groupBy( s => s.get("person").asInstanceOf[Option[Map[String,BigInt]]].get.get("id").get.toString )
  }
}