package practice.daily.tyler

import java.text.SimpleDateFormat
import java.util.{Calendar,Date,TimeZone}
import net.liftweb.json._
import net.liftweb.json.Extraction._
import org.specs2.mutable._

class ConsecutiveDaySpec extends Specification {

    implicit val formats = DefaultFormats // Brings in default date formats etc.

    "Consecutive Days" should {

        "be triggered with enough actions" in {

            val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))
            val rightNow = Calendar.getInstance();
            val nowDate = rightNow.getTime();
            
            val board = new Scoreboard()
            board.addAction(compact(render(decompose(
                Map(
                    "action" -> "goal-progress",
                    "timestamp" -> dateFormatter.format(Calendar.getInstance.getTime),
                    "person" -> Map(
                        "id" -> 21
                    )
                )
            ))))

            Thread.sleep(1000) // ES needs a bit of time to commit

            val oneCount = board.getActionCounts(21, "goal-progress")
            oneCount must beSome
            oneCount.get must havePair("goal-progress" -> 1)

            // Now add a few more
            (1 until 3) foreach { (x) => {
                val theCal = Calendar.getInstance;
                theCal.add(Calendar.DATE, -x)
                board.addAction(compact(render(decompose(
                    Map(
                        "action" -> "goal-progress",
                        "timestamp" -> dateFormatter.format(theCal.getTime),
                        "person" -> Map(
                            "id" -> 21
                        )
                    )
                ))))
            } }

            Thread.sleep(1000) // ES needs a bit of time to commit

            val twoCount = board.getActionCountsByDate(21, "goal-progress", 7)
            twoCount must beSome
            twoCount.get.keys.size mustEqual 3

             // And the rest

             (4 until 7) foreach { (x) => {
                 val theCal = Calendar.getInstance;
                 theCal.add(Calendar.DATE, -x)
                 board.addAction(compact(render(decompose(
                     Map(
                         "action" -> "goal-progress",
                         "timestamp" -> dateFormatter.format(theCal.getTime),
                         "person" -> Map(
                             "id" -> 21
                         )
                     )
                 ))))
             } }

             Thread.sleep(1000) // ES needs a bit of time to commit

             val threeCount = board.getActionCountsByDate(21, "goal-progress", 7)
             threeCount must beSome
             threeCount.get.keys.size mustEqual 6

             (7 until 9) foreach { (x) => {
                 val theCal = Calendar.getInstance;
                 theCal.add(Calendar.DATE, -x)
                 board.addAction(compact(render(decompose(
                     Map(
                         "action" -> "goal-progress",
                         "timestamp" -> dateFormatter.format(theCal.getTime),
                         "person" -> Map(
                             "id" -> 21
                         )
                     )
                 ))))
             } }

             val fourCount = board.getActionCountsByDate(21, "goal-progress", 3)
             fourCount must beSome
             fourCount.get.keys.size mustEqual 3

             board.purge(21) mustEqual(true)
        }
    }
}
