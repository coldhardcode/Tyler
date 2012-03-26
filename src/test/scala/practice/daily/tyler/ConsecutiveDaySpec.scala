package practice.daily.tyler

import practice.daily.tyler.utility.JSONLoader
import net.liftweb.json._
import org.scalatra.test.specs2._

import java.text.SimpleDateFormat
import java.util.{Calendar,Date}
import net.liftweb.json.JsonDSL._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class ConsecutiveDaySpec extends MutableScalatraSpec {

    implicit val formats = DefaultFormats // Brings in default date formats etc.

    addServlet(classOf[TylerServlet], "/*")

    "Servlet" should {

        "work with query" in {

            val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            val rightNow = Calendar.getInstance();
            val nowDate = rightNow.getTime();
            
            post("/user/2/action", compact(render(
                ("action" -> "goal-progress") ~
                ("timestamp" -> dateFormatter.format(nowDate)) ~
                ("person" -> (
                    "id" -> 2
                ))
            ))) {
                status mustEqual 200
            }

            Thread.sleep(1000) // ES needs a bit of time to commit

            get("/user/2/actions", Tuple("search", "goal-progress")) {
                 body mustEqual """{"goal-progress":1}"""
                 status mustEqual 200
            }

            // Now add a few more
            (1 until 3) foreach { (x) => {
                val theCal = Calendar.getInstance;
                theCal.add(Calendar.DATE, -x)
                post("/user/2/action", compact(render(
                    ("action" -> "goal-progress") ~
                    ("timestamp" -> dateFormatter.format(theCal.getTime)) ~
                    ("person" -> (
                        "id" -> 2
                    ))
                ))) {
                    status mustEqual 200
                }
            } }

            Thread.sleep(1000) // ES needs a bit of time to commit

            get("/user/2/actions", Tuple("search", "goal-progress")) {
                 body mustEqual """{"goal-progress":3}"""
                 status mustEqual 200
            }

             // And the rest

             (3 until 7) foreach { (x) => {
                 val theCal = Calendar.getInstance;
                 theCal.add(Calendar.DATE, -x)
                 post("/user/2/action", compact(render(
                     ("action" -> "goal-progress") ~
                     ("timestamp" -> dateFormatter.format(theCal.getTime)) ~
                     ("person" -> (
                         "id" -> 2
                     ))
                 ))) {
                     status mustEqual 200
                 }
             } }

             Thread.sleep(1000) // ES needs a bit of time to commit

             get("/user/2/actions", Tuple("search", "goal-progress")) {
                  body mustEqual """{"goal-progress":7}"""
                  status mustEqual 200
             }

             1 mustEqual 1
        }
    }
}
