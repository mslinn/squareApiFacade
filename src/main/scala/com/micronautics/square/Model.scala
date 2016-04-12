package com.micronautics.square

import java.util.TimeZone
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat.fullDateTime

case class Card(last4: String, cardBrand: String) {
  override def toString = s"$cardBrand ending with $last4"
}

case class CardDetails(entryMethod: String, card: Card, status: String) {
  override def toString = s"$card $entryMethod"
}

case class Money(amount: Int, currency: String) {
  override def toString = s"$amount $currency"
}

case class Refund(huh: String)

case class SquareLocation(
  name: String,
  id: String,
  timezone: TimeZone,
  capabilities: List[LocationCapability]
) {
  override def toString = s"""Location "$name" with id $id in time zone ${ timezone.getDisplayName } has capabilities: """ + capabilities.mkString(", ")
}

case class SquareTransaction(
  id: String,
  locationId: String,
  created: DateTime,
  product: SquareProduct,
  tenders: List[Tender]=Nil,
  referenceId: Option[String]=None,
  refunds: List[Refund]=Nil
) {
  override def toString = {
    val refId: String = referenceId.map( ri => s"with reference #$ri").mkString
    s"""Transaction #$id at location #$locationId created ${ fullDateTime.print(created) } by $product $refId
       |  ${ tenders.mkString("\n    ") }
       |  ${ refunds.mkString("\n    ") }
     """.stripMargin
  }
}

case class Tender(
  transactionId: String,
  cardDetails: CardDetails,
  created: DateTime,
  amount: Money,
  id: String,
  `type`: String,
  processingFee: Money,
  locationId: String
) {
  override def toString =
    s"""Tender #$id paid by ${`type`} $cardDetails for $amount with fee $processingFee""".stripMargin
}
