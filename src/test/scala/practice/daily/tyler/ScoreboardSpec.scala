package practice.daily.tyler

import org.specs2.mutable._
import redis.clients.jedis.{JedisPool,JedisPoolConfig}

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class ScoreboardSpec extends Specification {

    "Scoreboard" should {

        val pool = new JedisPool("localhost", 6379)
        val jedis = pool.getResource
        val board = new Scoreboard("2", jedis)
        
        "return a sane key" in {
            board.getUserKey("timeline") must be equalTo("user/2/timeline")
        }
        
        "add an action" in {
            board.addAction(name = "fart", action = "{\"foo\":\"bar\"}")
            
            jedis.get(board.getUserKey("action-count/fart")) must be equalTo("1")
        }

        // What the fuck
        jedis.disconnect
        jedis.disconnect


        // The above can't be last, so putting this to shut it up
        "wtf" in {
            1 must be equalTo(1)
        }
    }
}
