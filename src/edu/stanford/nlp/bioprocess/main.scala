package edu.stanford.nlp.bioprocess

import java.io.{File, FileOutputStream, PrintWriter}
import java.util
import scala.io.Source
import com.google.gson.GsonBuilder
import fig.basic.LogInfo
import fig.exec.Execution
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils._
import qa.util.FileUtil

import scala.collection.JavaConversions._

/**
  * Created by dick on 5/14/16.
  */
object main {
  def main(args: Array[String]) : Unit = {
    var mainObj = new edu.stanford.nlp.bioprocess.Main()
    Execution.init(args, mainObj)
    try {
      mainObj.run()
    } catch {
      case unknown: Throwable => Execution.raiseException(unknown)
        LogInfo.flush()
    }

    val results : Seq[BioDatum] = mainObj.getSRLPredictions()
    var gson = new GsonBuilder().registerTypeAdapter(classOf[BioDatum], new BioDatumSerializer()).create()
    val file = new File("/home/dick/srl/goto/lib/Dataset/train.ser")
    file.delete()
    val writer = new PrintWriter(file);
    for (d <- results) {
        writer.println(gson.toJson(d))
    }
    writer.close()
    gson = new GsonBuilder().registerTypeAdapter(classOf[BioDatumShell], new BioDatumDeserializer()).create()
    for (line <- Source.fromFile(file).getLines()) {
//        val d = gson.fromJson(line, classOf[BioDatumShell])
    }
    Execution.finish()
  }
}
