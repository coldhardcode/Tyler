package practice.daily.tyler

import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class TylerServletSpec extends MutableScalatraSpec {

  addServlet(classOf[TylerServlet], "/*")

  "GET / on Tyler" should {
    "return status 200" in {
      get("/") {
        status must_== 200
      }
    }
  }

  "GET of non-existent URL on Tyler" should {
    "return status 404" in {
      get("/isnthere") {
        status must_== 404
      }
    }
  }
  
  "POST to action" should {
    "create action status 200" in {
      post("/user/1/action", "{\"name\":\"completed-test\"}") {
        status must_== 200
      }
    }
  }
}
