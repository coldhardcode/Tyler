package practice.daily.tyler

import org.specs2.mutable._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class ElasticSearchSpec extends Specification {

    "ElasticSearch" should {

        val es = new ElasticSearch
        
        es.createIndex
        
        // The above can't be last, so putting this to shut it up
        "wtf" in {
            1 must be equalTo(1)
        }
    }
}
