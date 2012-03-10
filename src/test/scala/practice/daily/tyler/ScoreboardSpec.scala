package practice.daily.tyler

import org.specs2.mutable._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html 
class ScoreboardSpec extends Specification {

    "Scoreboard" should {

        val board = new Scoreboard("2")
        
        "return a sane key" in {
            board.getUserKey("timeline") must be equalTo("user/2/timeline")
        }
    }
}
