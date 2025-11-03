@file:Suppress("NoWildcardImports")

package websockets

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.websocket.ClientEndpoint
import jakarta.websocket.ContainerProvider
import jakarta.websocket.OnMessage
import jakarta.websocket.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.util.concurrent.CountDownLatch

private val logger = KotlinLogging.logger {}

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ElizaServerTest {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun onOpen() {
        logger.info { "This is the test worker" }
        val latch = CountDownLatch(3)
        val list = mutableListOf<String>()

        val client = SimpleClient(list, latch)
        client.connect("ws://localhost:$port/eliza")
        latch.await()
        assertEquals(3, list.size)
        assertEquals("The doctor is in.", list[0])
    }

    @Test
    fun onChat() {
        logger.info { "Test thread" }
        val latch = CountDownLatch(4)
        val list = mutableListOf<String>()

        val client = ComplexClient(list, latch)
        client.connect("ws://localhost:$port/eliza")
        latch.await()
        val size = list.size
        // 1. EXPLAIN WHY size = list.size IS NECESSARY
        // It is neccesary because you need to know if at least you receive 4 messages and at most 5 messages
        // 2. REPLACE BY assertXXX expression that checks an interval; assertEquals must not be used;
        assert(size == 4 || size == 5)
        // 3. EXPLAIN WHY assertEquals CANNOT BE USED AND WHY WE SHOULD CHECK THE INTERVAL
        // Because we cant say if we receive 4 or 5 messages, its a random value so we cant choose a specific number
        // 4. COMPLETE assertEquals(XXX, list[XXX])
        assertEquals("You don't seem very certain.", list[3])
        logger.info { "He recibido ${list.size}" }
    }
}

@ClientEndpoint
class SimpleClient(
    private val list: MutableList<String>,
    private val latch: CountDownLatch,
) {
    @OnMessage
    fun onMessage(message: String) {
        logger.info { "Client received: $message" }
        list.add(message)
        latch.countDown()
    }
}

@ClientEndpoint
class ComplexClient(
    private val list: MutableList<String>,
    private val latch: CountDownLatch,
) {
    @OnMessage
    @Suppress("UNUSED_PARAMETER") // Remove this line when you implement onMessage
    fun onMessage(
        message: String,
        session: Session,
    ) {
        logger.info { "Client received: $message" }
        list.add(message)
        latch.countDown()
        // 5. COMPLETE if (expression) {
        // 6. COMPLETE   sentence
        // }
        if (message == "---") {
            session.basicRemote.sendTextSafe("maybe")
        }
        logger.info { "He recibido en el cliente ${list.size}" }
    }
}

fun Any.connect(uri: String) {
    ContainerProvider.getWebSocketContainer().connectToServer(this, URI(uri))
}
