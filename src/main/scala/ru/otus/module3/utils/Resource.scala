package ru.otus.module3.utils

trait Resource{
  def close(): Unit
  def name: String
}

private class ResourceImpl(val name: String) extends Resource {
  println(s"Resource acquired ${name}")
  override def close(): Unit = println(s"Resource closed ${name}")
}

object Resource{
  def apply(name: String): Resource = new ResourceImpl(name)
}
