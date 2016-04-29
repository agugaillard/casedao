package com.github.agugaillard.casedao.utils

import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

import scala.util.{Failure, Success}

object JsonFormats {

  implicit object BSONObjectIDFormat extends Format[BSONObjectID] {
    def writes(objectId: BSONObjectID) = JsString(objectId.stringify)

    def reads(id: JsValue) = id match {
      case JsString(x) ⇒ {
        BSONObjectID.parse(x) match {
          case Success(y) ⇒ JsSuccess(y)
          case Failure(_) ⇒ JsError("Expected BSONObjectID as JsString")
        }
      }
      case _ ⇒ JsError("Expected BSONObjectID as JsString")
    }
  }

}
