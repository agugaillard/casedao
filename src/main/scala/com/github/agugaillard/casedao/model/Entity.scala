package com.github.agugaillard.casedao.model

import reactivemongo.bson.BSONObjectID

trait Entity {
  val id: BSONObjectID
}
