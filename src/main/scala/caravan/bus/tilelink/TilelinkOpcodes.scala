package caravan.bus.tilelink

trait OpCodes {
    val Get = 4
    val AccessAckData = 1
    val PutFullData = 0
    val PutPartialData = 1
    val AccessAck = 0
    val Arithmetic = 2
    val Logical = 3
    val Intent = 5
    val HintAck = 2
    val NoOp = 6
}