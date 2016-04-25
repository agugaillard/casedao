package com.github.agugaillard.dao.status

case class Status(code: Int, msg: String = "")

object Status {
  def OK(msg: String = "OK") = Status(200, msg)

  def Created(msg: String = "Created") = Status(201, msg)

  def NotFound(msg: String = "Not found") = Status(404, msg)

  def Conflict(msg: String = "Conflict") = Status(409, msg)
}
