package dag

import java.io.{ByteArrayOutputStream, File, FileInputStream, IOException}
import java.util.zip.GZIPOutputStream

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.net.URLCodec
import org.json4s.jackson.JsonMethods._

object Util {
  import org.json4s.DefaultFormats
  implicit val formats = DefaultFormats

  def read(file: File): DAG = {
    val stream = new FileInputStream(file.getCanonicalPath)
    val json = scala.io.Source.fromInputStream(stream).mkString
    parse(json).extract[DAG]
  }
  @throws(classOf[IOException])
  def compress(data: String): Array[Byte] = {
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream(data.length)
    val gzip: GZIPOutputStream = new GZIPOutputStream(bos)
    gzip.write(data.getBytes)
    gzip.close
    bos.close
    val compressed: Array[Byte] = bos.toByteArray
    return compressed
  }

  @throws(classOf[IOException])
  def compress(data: Array[Byte]): Array[Byte] = {
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream(data.length)
    val gzip: GZIPOutputStream = new GZIPOutputStream(bos)
    gzip.write(data)
    gzip.close
    bos.close
    val compressed: Array[Byte] = bos.toByteArray
    return compressed
  }

  def gravizoDotLink(dot: String): String = {
    "http://g.gravizo.com/g?" +
      new URLCodec().encode(dot).replace("+","%20")
  }

  def teachingmachinesDotLink(dot: String): String = {
    "http://graphvizserver-env.elasticbeanstalk.com/?" +
      new URLCodec().encode(
        Base64.encodeBase64URLSafeString(
          Util.compress(
            dot
          )
        ))
  }
}
