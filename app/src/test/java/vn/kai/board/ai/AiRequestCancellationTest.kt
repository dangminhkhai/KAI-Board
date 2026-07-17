package vn.kai.board.ai

import java.util.concurrent.CancellationException
import org.junit.Test

class AiRequestCancellationTest {
    @Test(expected = CancellationException::class)
    fun cancelledRequestStopsBeforeNetworkAttempt() {
        AiRequestCancellation().apply { cancel() }.check()
    }
}
