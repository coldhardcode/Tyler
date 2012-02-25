package practice.daily.tyler

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
      post("/user/1/action", "{\"name\":\"completed-test\"}") {
        status mustEqual 200
      }
    
      get("/user/1/actions", Tuple("search", "completed-test")) {
        status mustEqual 200
      }

      val params = List()
      delete("/user/1", params) {
        status mustEqual 200
      }
    }
    
  }
}
