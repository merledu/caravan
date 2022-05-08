package caravan.bus.tilelink

trait OpCodes {
    val Get = 4
    val AccessAckData = 1
    val PutFullData = 0
    val PutPartialData = 1
    val AccessAck = 0
    //  val Message     = OpCode    // Channel         Response Message
    val Acquire         = 6         //    A            Grant, GrantAck
    val Grant           = 4         //    D            GrantAck
    val GrantData       = 5         //    D            GrantAck
    val ProbeBlock      = 6         //    B            ProbeAck, ProbeAckData
    val ProbePerm       = 7         //    B            ProbeAck
    val ProbeAck        = 4         //    C            -
    val ProbeAckData    = 5         //    C            -
    val Release         = 6         //    C            ReleaseAck
    val ReleaseData     = 7         //    C            ReleaseAck
    val ReleaseAck      = 6         //    D            -
}