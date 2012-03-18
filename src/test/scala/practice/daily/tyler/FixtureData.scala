package practice.daily.tyler

import practice.daily.tyler.utility.JSONLoader
import net.liftweb.json._
import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class FixtureDataSpec extends MutableScalatraSpec {

    addServlet(classOf[TylerServlet], "/*")

    "Servlet" should {

        "return status 200 on GET of /" in {
            get("/") {
                status must_== 200
            }
        }

        "handle fixture data" in {
            JSONLoader.loadFromDirectory("/Users/gphat/foo")
            
            1 must be equalTo(1)
        }
    }
}
