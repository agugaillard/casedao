package com.github.agugaillard.casedao.macros

import com.github.agugaillard.casedao.dao.DAOConf
import com.github.agugaillard.casedao.lite.Lite
import com.github.agugaillard.casedao.model.{Entity, RFormat, R}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object RFormatMacro {
  def impl[A <: Entity](c: Context)(implicit wtt: c.WeakTypeTag[A]): c.Expr[RFormat[A]] = {
    import c.universe._

    val tpe = weakTypeOf[A]

    val fields = tpe.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.toSeq

    val entityFields = fields.filter { x =>
      x.typeSignature.resultType <:< typeOf[Lite[_]] || x.typeSignature.resultType <:< typeOf[Seq[Lite[_]]]
    }

    val entityFieldsAndTypes = entityFields.map { x =>
      if (x.typeSignature.resultType <:< typeOf[Seq[Lite[_]]])
        (x, x.typeSignature.resultType.typeArgs.head.resultType.typeArgs.head)
      else
        (x, x.typeSignature.resultType.typeArgs.head)
    }

    val entityFieldsAndTypesAndCompanions = entityFieldsAndTypes.map { x =>
      (x._1, x._2, x._2.typeSymbol.companion)
    }

    val entityFieldsAndTypesAndCompanionsAndModelConfs = entityFieldsAndTypesAndCompanions.map { x =>
      val s = appliedType(typeOf[DAOConf[_]], x._2 :: Nil)
      val iconf = c.inferImplicitValue(s).symbol
      (x._1, x._2, x._3, iconf)
    }

    val f = entityFieldsAndTypesAndCompanionsAndModelConfs.map { x =>
      val y = x._2.members.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      }.toSeq.filter { z =>
        (z.typeSignature.resultType <:< weakTypeOf[Lite[A]] || z.typeSignature.resultType <:< weakTypeOf[Seq[Lite[A]]]) && z.isPublic
      }.head
      (x._1, x._2, x._3, x._4, y)
    }

    val params = f.map { r =>
      val content = r._1
      val n = !(r._1.typeSignature.resultType <:< typeOf[Lite[_]])
      val fieldName = r._1.name.toString
      val outsidefn = r._5.name.toString
      val daoconf = r._4
      val outsiden = !(r._4.typeSignature.resultType <:< typeOf[Lite[_]])
      (content, fieldName, n, outsidefn, daoconf, outsiden)
    }

    val rType = weakTypeOf[R[_]]
    val rTypeCompanion = rType.typeSymbol.companion

    val rs = params.map { case (content, fieldName, n, outsidefn, daoconf, outsiden) =>
      if (n)
        q"""$rTypeCompanion(o.$content.map(_.id), $fieldName, $n, $outsidefn, $daoconf, $outsiden)"""
      else
        q"""$rTypeCompanion(Seq(o.$content.id), $fieldName, $n, $outsidefn, $daoconf, $outsiden)"""
    }
    c.Expr[RFormat[A]](q"""new RFormat[$tpe] { def rels(o: $tpe) = Seq(..$rs) }""")
  }
}
