package practice.daily.tyler

import org.specs2.mutable._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class ElasticSearchSpec extends Specification {

    "ElasticSearch" should {

        val es = new ElasticSearch("test123")
        
        "should return false for a non-existent index" in {
            es.verifyIndex must be equalTo(false)
        }
        
        es.createIndex

        "should return true for an existing index" in {
            es.verifyIndex must be equalTo(true)
        }
        
        es.deleteIndex
        
        // The above can't be last, so putting this to shut it up
        "wtf" in {
            1 must be equalTo(1)
        }
    }
}
