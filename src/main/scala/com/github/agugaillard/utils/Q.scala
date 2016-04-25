package com.github.agugaillard.utils

import com.github.agugaillard.lite.{CLite, Lite}
import com.github.agugaillard.model.Entity
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONObjectID

object Q {
  def get(id: String) = BSONDocument("_id" -> BSONObjectID(id))

  def get(ids: Seq[String]) = BSONDocument("_id" -> BSONDocument("$in" -> ids.map(BSONObjectID(_))))

  def get(id: BSONObjectID) = BSONDocument("_id" -> id)

  def proj(fs: Seq[String]) = fs.foldLeft(BSONDocument())((acum, f) => acum ++ BSONDocument(f -> 1))

  def slice(field: String, value: Int) = BSONDocument(field -> BSONDocument("$slice" -> value))

  def all(condition: BSONDocument = BSONDocument()) = condition

  def setOnInsert[A](o: A)(implicit writer: BSONDocumentWriter[A]) = BSONDocument("$setOnInsert" -> o)

  def addToSet[A <: Entity](field: String, doc: Lite[A])(implicit clite: CLite[A]) = BSONDocument("$addToSet" -> BSONDocument(field -> doc))

  def push[A](field: String, value: A)(implicit w: BSONDocumentWriter[A]) = BSONDocument("$push" -> BSONDocument(field -> value))

  def pull(field: String, id: BSONObjectID) = BSONDocument("$pull" -> BSONDocument(field -> BSONDocument("_id" -> id)))

  def delete(implicit w: BSONDocumentWriter[Boolean]) = Q.set("audit.delete.deleted", true)

  def restore(implicit w: BSONDocumentWriter[Boolean]) = Q.set("audit.delete.deleted", false)

  def set(doc: BSONDocument) = BSONDocument("$set" -> doc)

  def set(l: Lite[_]) = BSONDocument("$set" -> l.doc)

  def unset(field: String) = BSONDocument("$unset" -> BSONDocument(field -> 1))

  def set[A](field: String, value: A)(implicit w: BSONDocumentWriter[A]) = BSONDocument("$set" -> BSONDocument(field -> value))

  def ne(doc: BSONDocument) = doc.elements.foldLeft(BSONDocument())((acum, e) => acum ++ BSONDocument(e._1 -> BSONDocument("$ne" -> e._2)))

  def map(input: String, as: String, in: BSONDocument) = BSONDocument("$map" -> BSONDocument("input" -> input, "as" -> as, "in" -> in))
}
