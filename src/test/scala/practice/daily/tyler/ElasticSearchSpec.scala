package practice.daily.tyler

import org.specs2.mutable._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class ElasticSearchSpec extends Specification {

    "ElasticSearch" should {

        "should be able to check index status" in {
            val es = new ElasticSearch("test123")
            es.verifyIndex must be equalTo(false)
            es.createIndex must be equalTo(true)
            es.verifyIndex must be equalTo(true)
            es.deleteIndex must be equalTo(true)
        }    

        "should be able to check index status" in {
            val es = new ElasticSearch("test123xxx")
            es.deleteIndex must be equalTo(false)
        }    
    }
}
