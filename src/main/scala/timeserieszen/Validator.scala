package com.timeserieszen

import scalaz._
import Scalaz._

// see http://meta.plasm.us/posts/2013/06/05/applicative-validation-syntax/

object Validator {
  type ErrorsOr[A] = ValidationNel[String, A]
  type Validator[A] = String => ErrorsOr[A]

  def alternative[A](o: Option[A])(s: String): ValidationNel[String,A] = o.map(_.successNel).getOrElse(s.failureNel[A])

  final implicit class ValidatorOps[A](val self: Option[A]) extends AnyVal {
    def <|>(s: String): ValidationNel[String,A] = alternative(self)(s)
  }
}
