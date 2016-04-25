package com.github.agugaillard.model

import reactivemongo.bson.BSONObjectID

trait Entity {
  val id: BSONObjectID
}
