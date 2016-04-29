package com.github.agugaillard.casedao.macros

import com.github.agugaillard.casedao.lite.Lite
import com.github.agugaillard.casedao.model.Entity

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

object LFormatMacro {
  def impl[A <: Entity](c: Context)(implicit wtt: c.WeakTypeTag[A]): c.Tree = {
    import c.universe._

    val tpe = weakTypeOf[A]

    val fields = tpe.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.toSeq

    val notEntityFields = fields.filterNot { x =>
      x.typeSignature.resultType <:< typeOf[Lite[_]] || x.typeSignature.resultType <:< typeOf[Seq[Lite[_]]]
    }

    val fieldsName = notEntityFields.filterNot(_.name.toString == "id").map(_.name.toString)

    q"""LFormat[$tpe](Seq(..$fieldsName, "_id"))"""
  }
}
