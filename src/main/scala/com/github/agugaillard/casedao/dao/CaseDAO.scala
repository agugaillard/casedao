package com.github.agugaillard.casedao.dao

import com.github.agugaillard.casedao.dao.status.Status
import com.github.agugaillard.casedao.lite.{LFormat, Lite}
import com.github.agugaillard.casedao.model.{Entity, R, RFormat}
import com.github.agugaillard.casedao.utils.Q
import reactivemongo.bson._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CaseDAO {
  def all[A <: Entity](implicit r: BSONDocumentReader[A], conf: DAOConf[A]) =
    conf.collection.find(BSONDocument()).cursor[A]().collect[Seq]()

  def save[A <: Entity](o: A)(implicit r: BSONDocumentReader[A], w: BSONDocumentWriter[A], lite: LFormat[A], conf: DAOConf[A], rf: RFormat[A]) =
    conf.collection.findAndUpdate(
      conf.constraint(o),
      Q.setOnInsert(o),
      true,
      true).map { x =>
      val result = x.result[A].get
      (result, {
        if (o.id == result.id) {
          add(rf.rels(o), Lite(o))
          Status.Created()
        } else Status.Conflict()
      })
    }

  def update[A <: Entity](o: A, lite: Boolean = true)(implicit r: BSONDocumentReader[A], w: BSONDocumentWriter[A], clite: LFormat[A], conf: DAOConf[A], rf: RFormat[A]) = {
    conf.collection.find(conf.constraint(o)).one[A].flatMap { x =>
      x match {
        case None =>
          val l = Lite(o)
          conf.collection.findAndUpdate(
            Q.get(o.id),
            Q.set({
              if (lite) l.doc else BSON.write(o)
            }),
            false).map { y =>
            y.value match {
              case Some(_) =>
                updatein(rf.rels(o), l)
                Status(204)
              case None => Status.NotFound()
            }
          }
        case Some(e) => Future {
          Status(400)
        }
      }
    }
  }

  def get[A <: Entity](id: String)(implicit r: BSONDocumentReader[A], conf: DAOConf[A]) =
    conf.collection.find(Q.get(id)).one[A].map { x =>
      x match {
        case Some(e) => (x, Status.OK())
        case None => (x, Status.NotFound())
      }
    }

  def get[A <: Entity](id: String, proj: String*)(implicit r: BSONDocumentReader[A], clite: LFormat[A], conf: DAOConf[A], projector: BSONDocument => A = (d: BSONDocument) => {}) =
    conf.collection.find(Q.get(id), Q.proj(proj) ++ Q.proj(clite.attrs)).one[A].map { x =>
      x match {
        case Some(e) => (x, Status.OK())
        case None => (x, Status.NotFound())
      }
    }

  def get[A <: Entity](ids: Seq[String])(implicit r: BSONDocumentReader[A], conf: DAOConf[A]) =
    conf.collection.find(Q.get(ids)).cursor[A]().collect[Seq]()

  def get[A <: Entity](ids: Seq[String], proj: String*)(implicit r: BSONDocumentReader[A], clite: LFormat[A], conf: DAOConf[A]) =
    conf.collection.find(Q.get(ids), Q.proj(proj) ++ Q.proj(clite.attrs)).cursor[A]().collect[Seq]()

  def lite[A <: Entity](id: String)(implicit clite: LFormat[A], conf: DAOConf[A]) =
    conf.collection.find(Q.get(id), Q.proj(clite.attrs)).one[Lite[A]]

  def lite[A <: Entity](ids: Seq[String])(implicit clite: LFormat[A], conf: DAOConf[A]) =
    conf.collection.find(Q.get(ids), Q.proj(clite.attrs)).cursor[Lite[A]]().collect[Seq]()

  def deplace[A <: Entity](e: A, o: A, join: (A, A) => A = (a: A, b: A) => b)(implicit w: BSONDocumentWriter[A], clite: LFormat[A], conf: DAOConf[A], rels: A => Seq[R[_ <: Entity]]) = {
    conf.collection.update(Q.get(o.id), join(e, o)).flatMap { _ =>
      conf.collection.remove(Q.get(e.id), firstMatchOnly = true).flatMap { _ =>
        updatein(e.id, rels(e), Lite(o))
      }
    }
  }

  def deplace[A <: Entity](e: A, by: Seq[(A, (A, A) => A, String)])(implicit w: BSONDocumentWriter[A], clite: LFormat[A], conf: DAOConf[A], rels: A => Seq[R[_ <: Entity]]) = {
    val rs = rels(e)
    val fields = by.map(_._3)
    val (replaceRels, deleteRels) = rs.partition(r => fields.contains(r.outsideFieldName))
    Future.sequence {
      by.map {
        case (o, join, field) =>
          val j = join(e, o)
          conf.collection.update(Q.get(o.id), j).flatMap(_ =>
            updatein(e.id, replaceRels, Lite(j)).flatMap(_ =>
              delete(e, deleteRels)))
      }
    }
  }

  def delete[A <: Entity](o: A)(implicit conf: DAOConf[A], rels: A => Seq[R[_ <: Entity]]) =
    conf.collection.remove(Q.get(o.id), firstMatchOnly = true).flatMap { _ =>
      Future.sequence(rels(o).map(deletein(o.id, _)))
    }

  private def delete[A <: Entity](o: A, rels: Seq[R[_ <: Entity]])(implicit conf: DAOConf[A]) =
    conf.collection.remove(Q.get(o.id), firstMatchOnly = true).flatMap { _ =>
      Future.sequence(rels.map(deletein(o.id, _)))
    }

  private def add[A <: Entity](rels: Seq[R[_ <: Entity]], le: Lite[A])(implicit lite: LFormat[A]) =
    Future.sequence(rels.map { r =>
      val ids = r.ids.map(_.stringify)
      r.outsideDAOConf.collection.update(
        Q.get(ids),
        Q.push(r.outsideFieldName, le),
        multi = r.n)
    })

  private def updatein[A <: Entity](rels: Seq[R[_ <: Entity]], le: Lite[A])(implicit lite: LFormat[A]) =
    Future.sequence(rels.map { r =>
      r.outsideDAOConf.collection.update(
        BSONDocument(r.fieldName + "._id" -> le.id),
        Q.set({
          if (r.outsideN) r.fieldName + ".$" else r.fieldName
        }, le),
        multi = r.n)
    })

  private def updatein[A <: Entity](id: BSONObjectID, rels: Seq[R[_ <: Entity]], le: Lite[A])(implicit lite: LFormat[A]) =
    Future.sequence(rels.map { r =>
      r.outsideDAOConf.collection.update(
        BSONDocument(r.fieldName + "._id" -> id),
        Q.set(r.fieldName + {
          if (r.outsideN) ".$" else ""
        }, le),
        multi = r.n)
    })

  private def deletein(id: BSONObjectID, rel: R[_ <: Entity]) =
    rel.outsideDAOConf.collection.update(
      BSONDocument(rel.fieldName + "._id" -> id),
      if (rel.outsideN) Q.pull(rel.fieldName, id) else Q.unset(rel.fieldName),
      multi = rel.n)
}
