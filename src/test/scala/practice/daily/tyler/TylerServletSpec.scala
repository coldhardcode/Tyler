package practice.daily.tyler

import net.liftweb.json._
import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class TylerServletSpec extends MutableScalatraSpec {

    addServlet(classOf[TylerServlet], "/*")

    "Servlet" should {

        "return status 200 on GET of /" in {
            get("/") {
                status must_== 200
            }
        }

        "return status 404 on GET of none-existent URL" in {
            get("/isnthere") {
                status mustEqual 404
            }
        }

        "have a sane lifecycle" in {
            post("/user/1/action", """{"action":"completed-test","person":{"id":1}}""") {
                status mustEqual 200
            }

            Thread.sleep(1000) // ES needs a bit of time to commit

            get("/user/1/timeline") {
                body mustEqual """[{"action":"completed-test","person":{"id":1}}]"""
                status mustEqual 200
            }
            
            get("/user/1/actions", Tuple("search", "completed-test")) {
                body mustEqual "{\"completed-test\":1}"
                status mustEqual 200
            }
            
            // Post a second action
            post("/user/1/action", """{"action":"completed-test","person":{"id":1}}""") {
                status mustEqual 200
            }
            
            Thread.sleep(1000) // ES needs a bit of time to commit
            
            get("/user/1/timeline") {
                body mustEqual """[{"action":"completed-test","person":{"id":1}},{"action":"completed-test","person":{"id":1}}]"""
                status mustEqual 200
            }
            
            // Check the count again, should be 2
            get("/user/1/actions", Tuple("search", "completed-test")) {
                body mustEqual "{\"completed-test\":2}"
                status mustEqual 200
            }
            
            // Now add a second action
            post("/user/1/action", """{"action":"completed-test2","person":{"id":1}}""") {
                status mustEqual 200
            }
            
            Thread.sleep(1000) // ES needs a bit of time to commit
            
            // Check the count again, should be 2
            get("/user/1/actions", Tuple("search", "completed*")) {
            
                val counts = JsonParser.parse(body)
                // Verify counts of each are returned properly
                counts.values.asInstanceOf[Map[String,Any]]("completed-test") mustEqual 2
                counts.values.asInstanceOf[Map[String,Any]]("completed-test2") mustEqual 1
            
                status mustEqual 200
            }
            
            val params = List()
            delete("/user/1", params) {
                status mustEqual 200
            }
        }
    }
}
