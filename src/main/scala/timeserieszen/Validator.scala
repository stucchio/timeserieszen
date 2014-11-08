package com.timeserieszen

import scalaz._
import Scalaz._

// see http://meta.plasm.us/posts/2013/06/05/applicative-validation-syntax/

object Validator {
  type ErrorsOr[A] = ValidationNel[String, A]
  type Validator[A] = String => ErrorsOr[A]

  def alternative[A](o: Option[A])(s: String): ValidationNel[String,A] = o match {
    case Some(a) => a.success
    case None => s.failureNel[A]
  }
}
