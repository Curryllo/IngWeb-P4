@file:Suppress("NoWildcardImports")

package websockets

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.websocket.ClientEndpoint
import jakarta.websocket.ContainerProvider
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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

    @Test
    fun broadcast() {
        logger.info { "This is the broadcast test" }
        var latch1 = CountDownLatch(3)
        val list1 = mutableListOf<String>()

        var latch2 = CountDownLatch(3)
        val list2 = mutableListOf<String>()

        val client1 = SimpleClient(list1, latch1)
        client1.connect("ws://localhost:$port/eliza")
        latch1.await()
        assertEquals(3, list1.size)

        val client2 = SimpleClient(list2, latch2)
        client2.connect("ws://localhost:$port/eliza")
        latch2.await()
        assertEquals(3, list2.size)

        latch1 = CountDownLatch(1)
        latch2 = CountDownLatch(1)

        client1.latch = latch1
        client2.latch = latch2

        client1.session.basicRemote.sendTextSafe("maybe")

        latch1.await()
        latch2.await()

        assertEquals("You don't seem very certain.", list1[3])
        assertEquals("You don't seem very certain.", list2[3])
    }
}

@ClientEndpoint
class SimpleClient(
    private val list: MutableList<String>,
    var latch: CountDownLatch,
) {
    lateinit var session: Session

    @OnOpen
    fun onOpen(session: Session) {
        this.session = session
    }

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
    fun onMessage(
        message: String,
        session: Session,
    ) {
        logger.info { "Client received: $message" }
        list.add(message)
        latch.countDown()
        if (message == "---") {
            session.basicRemote.sendTextSafe("maybe")
        }
        logger.info { "He recibido en el cliente ${list.size}" }
    }
}

@ClientEndpoint
class SemiComplexClient(
    private val list: MutableList<String>,
    private val latch: CountDownLatch,
)

fun Any.connect(uri: String) {
    ContainerProvider.getWebSocketContainer().connectToServer(this, URI(uri))
}
