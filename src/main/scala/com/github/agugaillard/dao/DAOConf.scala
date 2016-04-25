package com.github.agugaillard.dao

import com.github.agugaillard.model.Entity
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

case class DAOConf[A <: Entity](val collection: BSONCollection, val uniq: A => BSONDocument)
