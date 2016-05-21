import java.io.{BufferedWriter, File, FileWriter}

import org.apache.commons.io.FileUtils
import qa.util.FileUtil
import sbu.srl.datastructure.{ArgumentSpan, Sentence}
import java.util.ArrayList
import java.util.regex.{Matcher, Pattern}

import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConversions._
import scala.util.matching.Regex

/**
  * Created by dick on 5/14/16.
  */
object sam2brat {
  def main(args:Array[String]) {
    var sentences: ArrayList[Sentence] = FileUtil.deserializeFromFile("/home/dick/srl/knowledge-extract/train/train.ser").asInstanceOf[ArrayList[Sentence]]
//    val dir: File = new File("/home/dick/srl/goto/lib/Dataset/train/")
//    FileUtils.deleteDirectory(dir)
//    dir.mkdir()

    var i = 0
    for (x <- sentences) {
      i += 1
      var txt = new File(s"/home/dick/srl/goto/lib/Dataset/train/s$i.txt")
      var bw = new BufferedWriter(new FileWriter(txt))
      val sent = x.getRawText()
      bw.write(sent + '\n')
      bw.close()
      var y: ArrayList[ArgumentSpan] = x
        .getAllAnnotatedArgumentSpan()
      var later = new ArrayList[StringBuilder]
      var events = new ArrayList[String]
      var entities = new ArrayList[String]
      later.add(new StringBuilder(""))
      var seen = Set[String]()
      for (z <- y) {
        if (z.getAnnotatedLabel().toInt == 1) {
          var span = z.getText()
          var s = sent.indexOf(span)
          if (s == -1) {
            var w = z.getStartIdx()
            s = StringUtils.ordinalIndexOf(sent, " ", w - 1) + 1
          }
          span = span.replaceAll("-[LR]RB-", "")
          val pattern = """.*?(\w+)\W*$""".r
          val pattern(lastW) = span
          var hack = if (z.getEndIdx() != z.getStartIdx()) sent.indexOf(" ", s) else s
          var e = sent.indexOf(lastW, hack) + lastW.length()

          var role = z.getAnnotatedRole()
          if (role == "trigger") {
            var sz = later.size()
            var k = events.size() + 1
            var already = !later(sz - 1).toString().isEmpty && later(sz - 1).toString().charAt(0) == 'E'
            if (already) {
              sz += 1
              later.add(new StringBuilder(s"E$sz\tEvent:T$k"))
            } else {
              later.get(sz - 1).insert(0, s"E$sz\tEvent:T$k")
            }
            events.add(s"T$k\tEvent $s $e\t$span")
          } else if (!seen.contains(role)) {
            seen += role
            var k = 100 + entities.size() + 1
            entities.add(s"T$k\tEntity $s $e\t$span")
            var sz = later.size()
            val m = Map("undergoer" -> "agent", "enabler" -> "raw-material", "result" -> "result")
            /* if (!later(sz - 1).isEmpty)
              later(sz - 1).append(" ") */
            later(sz - 1).append(s" ${m(role)}:T$k")
          }
        }
      }

      var ann = new File(s"/home/dick/srl/goto/lib/Dataset/train/s$i.ann")
      bw = new BufferedWriter(new FileWriter(ann))
      val pattern = """.*Event:(\S+).*""".r
      var found = false
      for (l <- later) {
        if (l.toString().split("\\s+").length > 2 && l.toString().charAt(0) == 'E') {
          val pattern(a) = l.toString()
          for (e <- events) {
            if (e.substring(0,2) == a) {
              bw.write(e + '\n')
              bw.write(l.toString() + '\n')
              found = true
            }
          }
        }
      }
      for (e <- entities) {
        bw.write(e + '\n')
      }
      bw.close()
      if (!found) {
        txt.delete()
        ann.delete()
      }
    }
  }
}

