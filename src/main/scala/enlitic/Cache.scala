package enlitic

import java.io._

import scala.io.Source

/**
  * Created by ptsier on 12/12/15.
  */
trait Cache[T] {
  def store(key: String, value: T): Unit

  def getOpt(key: String): Option[T]

  def get(key: String)(f: => T): T = {
    getOpt(key) getOrElse {
      val res = f
      store(key, res)
      res
    }
  }
}


abstract class FileCache[T](dir: String) extends Cache[T] {
  {
    val cacheDir = new File(dir)
    if (!cacheDir.exists()) {
      cacheDir.mkdirs()
    }
  }

  def write(f: File, t: T)

  def read(f: File): T

  override def store(key: String, value: T): Unit = {
    val file = new File(dir, key)
    write(file, value)
  }

  override def getOpt(key: String): Option[T] = {
    val file = new File(dir, key)
    if (file.exists()) {
      Some(read(file))
    } else {
      None
    }
  }
}

class StringFileCache(dir: String) extends FileCache[String](dir) {
  override def write(f: File, t: String) {
    val writer = new FileWriter(f)
    writer.write(t)
    writer.close()
  }

  override def read(f: File): String = {
    Source.fromFile(f).getLines().mkString("\n")
  }
}

class ObjectFileCache[T](dir: String) extends FileCache[T](dir) {
  override def write(f: File, t: T): Unit = {
    val fOut = new FileOutputStream(f)
    val out = new ObjectOutputStream(fOut)
    out.writeObject(t)
    out.close()
    fOut.close()
  }

  override def read(f: File): T = {
    val fIn = new FileInputStream(f)
    val in = new ObjectInputStream(fIn)
    val res = in.readObject().asInstanceOf[T]
    fIn.close()
    in.close()
    res
  }
}

