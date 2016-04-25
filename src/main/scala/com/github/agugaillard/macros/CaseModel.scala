package com.github.agugaillard.macros

import com.github.agugaillard.dao.DAOConf
import com.github.agugaillard.model.{Entity, R}

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

object CaseModelMacro {
  def asd[A <: Entity]: (A) => Seq[R[_]] = macro impl[A]

  def impl[A <: Entity](c: Context)(implicit wtt: c.WeakTypeTag[A]): c.Expr[(A) => Seq[R[_]]] = {
    import c.universe._

    val tpe = weakTypeOf[A]

    val fields = tpe.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.toSeq

    val entityFields = fields.filter { x =>
      x.typeSignature.resultType <:< typeOf[Entity] || x.typeSignature.resultType <:< typeOf[Seq[Entity]]
    }

    val entityFieldsAndTypes = entityFields.map { x =>
      if (x.typeSignature.resultType <:< typeOf[Seq[Entity]])
        (x, x.typeSignature.resultType.typeArgs.head)
      else
        (x, x.typeSignature.resultType)
    }

    val entityFieldsAndTypesAndCompanions = entityFieldsAndTypes.map { x =>
      (x._1, x._2, x._2.typeSymbol.companion)
    }

    val entityFieldsAndTypesAndCompanionsAndModelConfs = entityFieldsAndTypesAndCompanions.map { x =>
      (x._1, x._2, x._3, x._3.typeSignature.members.filter { y => y.typeSignature.resultType <:< typeOf[DAOConf[_]] && y.isPublic }.head)
    }

    val f = entityFieldsAndTypesAndCompanionsAndModelConfs.map { x =>
      val y = x._2.members.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      }.toSeq.filter { z =>
        (z.typeSignature.resultType <:< weakTypeOf[A] || z.typeSignature.resultType <:< weakTypeOf[Seq[A]]) && z.isPublic
      }.head
      (x._1, x._2, x._3, x._4, y)
    }

    val params = f.map { r =>
      val content = r._1
      val n = !(r._1.typeSignature.resultType <:< typeOf[Entity])
      val fieldName = r._1.name.toString
      val outsidefn = r._5.name.toString
      val daoconf = r._4
      val outsiden = !(r._4.typeSignature.resultType <:< typeOf[Entity])
      (content, fieldName, n, outsidefn, daoconf, outsiden)
    }

    c.Expr[(A) => Seq[R[_]]] {
      //      q"""R($content, $fieldName, $n, $outsidefn, $daoconf, $outsiden)"""
      val qwe = params.map { case (content, fieldName, n, outsidefn, daoconf, outsiden) =>
        if (n)
          q"""R(o.$content, $fieldName, $n, $outsidefn, $daoconf, $outsiden)"""
        else
          q"""R(Seq(o.$content), $fieldName, $n, $outsidefn, $daoconf, $outsiden)"""
      }
      q"""(o: $tpe) => Seq(..$qwe)"""
    }
  }
}
