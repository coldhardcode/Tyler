package practice.daily.tyler.utility

import practice.daily.tyler.Scoreboard

import java.io.{File,FileInputStream}
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

object JSONLoader {

    def loadFromDirectory(scoreboard : Scoreboard, path : String) {

        val dir = new File(path)
        val files = dir.listFiles.filter {
            file => file.toString endsWith(".json")
        } foreach {
            file => postFile(scoreboard = scoreboard, file = file)
        }
    }
    
    def postFile(scoreboard : Scoreboard, file : File) {
        
        val stream = new FileInputStream(file)
        val channel = stream.getChannel
        val bb = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size)
        val contents = Charset.forName("UTF-8").decode(bb).toString
        println("Indexing...")
        scoreboard.addAction(contents)
    }
}