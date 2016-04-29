package com.github.agugaillard.casedao.dao

import com.github.agugaillard.casedao.model.Entity
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

case class DAOConf[A <: Entity](val collection: BSONCollection, val constraint: A => BSONDocument)
