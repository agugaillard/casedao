package com.github.agugaillard.casedao.lite

import com.github.agugaillard.casedao.macros.LFormatMacro
import com.github.agugaillard.casedao.model.Entity
import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.play.json.BSONFormats.BSONDocumentFormat

import scala.language.experimental.macros

trait Lite[A <: Entity] {
  val doc: BSONDocument

  override def toString = doc.toString

  def id = doc.getAs[BSONObjectID]("_id").get
}

object Lite {
  def conf[A <: Entity](attrs: String*) = LFormat[A](attrs ++ Seq("_id"))

  def conf[A <: Entity]: LFormat[A] = macro LFormatMacro.impl[A]

  def apply[A <: Entity](o: A)(implicit lf: LFormat[A], w: BSONDocumentWriter[A]): Lite[A] = new Lite[A] {
    val doc = lf.trim(BSON.writeDocument(o))
  }

  def apply[A <: Entity](d: BSONDocument)(implicit lf: LFormat[A]): Lite[A] = new Lite[A] {
    val doc = lf.trim(d)
  }

  implicit def bson[A <: Entity](implicit lf: LFormat[A]) = new BSONDocumentWriter[Lite[A]] with BSONDocumentReader[Lite[A]] {
    def read(d: BSONDocument) = Lite[A](d)

    def write(o: Lite[A]) = o.doc
  }

  implicit def json[A <: Entity](implicit lf: LFormat[A]): OFormat[Lite[A]] = new OFormat[Lite[A]] {
    def reads(js: JsValue) = {
      val b = Json.fromJson[BSONDocument](js).get
      JsSuccess(Lite(BSONDocument("_id" -> BSONObjectID(b.getAs[String]("id").get)) ++ b.remove("id")))
    }

    def writes(o: Lite[A]) = {
      val jo = Json.toJson(o.doc).as[JsObject]
      val id = (jo.fields.find { f => f._1 == "_id" }.get._2.as[JsObject] \ "$oid").as[JsString]
      JsObject(Seq(("id", id)) ++ jo.fields.filterNot { x => x._1 == "_id" })
    }
  }
}

case class LFormat[A <: Entity](attrs: Seq[String]) {
  def trim(d: BSONDocument): BSONDocument =
    d.remove(d.elements.filterNot { e => attrs.contains(e._1) }.map { e => e._1 }: _*)
}
