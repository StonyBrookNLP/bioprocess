import org.apache.commons.lang3.StringUtils
import com.google.gson.Gson
import _root_.edu.stanford.nlp.bioprocess.ArgumentRelation

import scala.collection.JavaConversions._

object worksheet {
  var l = List("foo", "bar", "baz")
  var lpairs = l zip  l
  new Gson().toJson(List(1,2))

  val regex = "(first):(second)".r
  val regex(a,b) = "first:second"
  println(ArgumentRelation.getSemanticRoles())
  import qa.util.FileUtil
  import sbu.srl.datastructure.{ArgumentSpan, Sentence}
  import java.util.ArrayList
  object helper {
    var sentences: ArrayList[Sentence] = FileUtil.deserializeFromFile("/home/dick/srl/knowledge-extract/train/train.ser").asInstanceOf[ArrayList[Sentence]]
  }
  //helper.sentences.length
/*  for (x <- helper.sentences) {
    println(x.getRawText())
    var y: ArrayList[ArgumentSpan] = x
      .getAllAnnotatedArgumentSpan()
    for (z <- y) {
      println(s"Span ${z.getText()},  Role ${z.getAnnotatedRole()}")
    }
  }*/
  StringUtils.ordinalIndexOf("baby people word", " ", 1)
}
