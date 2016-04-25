package com.github.agugaillard.model

import com.github.agugaillard.dao.DAOConf
import com.github.agugaillard.lite.Lite

case class R[A <: Entity](content: Seq[Lite[A]],
                          fieldName: String,
                          n: Boolean,
                          outsideFieldName: String,
                          outsideDAOConf: DAOConf[A],
                          outsideN: Boolean)
