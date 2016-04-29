package com.github.agugaillard.casedao.model

import com.github.agugaillard.casedao.dao.DAOConf
import com.github.agugaillard.casedao.macros.RFormatMacro
import reactivemongo.bson.BSONObjectID

import scala.language.experimental.macros

case class R[A <: Entity](ids: Seq[BSONObjectID],
                          fieldName: String,
                          n: Boolean,
                          outsideFieldName: String,
                          outsideDAOConf: DAOConf[A],
                          outsideN: Boolean)

trait RFormat[A <: Entity] {
  def rels(o: A): Seq[R[_ <: Entity]]
}

object RFormat {
  def get[A <: Entity]: RFormat[A] = macro RFormatMacro.impl[A]
}
