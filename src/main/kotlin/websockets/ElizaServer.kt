@file:Suppress("NoWildcardImports", "WildcardImport", "SpreadOperator")

package websockets

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.websocket.CloseReason
import jakarta.websocket.CloseReason.CloseCodes
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.RemoteEndpoint
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpoint
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.util.Locale
import java.util.Scanner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
/*
@Configuration(proxyBeanMethods = false)
class WebSocketConfig {
    @Bean
    fun serverEndpoint() = ServerEndpointExporter()
}
*/

private val logger = KotlinLogging.logger {}

/**
 * If the websocket connection underlying this [RemoteEndpoint] is busy sending a message when a call is made to send
 * another one, for example if two threads attempt to call a send method concurrently, or if a developer attempts to
 * send a new message while in the middle of sending an existing one, the send method called while the connection
 * is already busy may throw an [IllegalStateException].
 *
 * This method wraps the call to [RemoteEndpoint.Basic.sendText] in a synchronized block to avoid this exception.
 */
fun RemoteEndpoint.Basic.sendTextSafe(message: String) {
    synchronized(this) {
        sendText(message)
    }
}

@ServerEndpoint("/stats")
@Component
class StatsEndpoint {
    companion object {
        private val sessions = ConcurrentHashMap.newKeySet<Session>()
        private val connectedUsers = AtomicInteger(0)
        private val messagesSent = AtomicInteger(0)

        fun broadcastStats() {
            val stats = """{
                "connectedUsers": ${connectedUsers.get()},
                "messagesSent": ${messagesSent.get()}
            }"""
            for (s in sessions) {
                if (s.isOpen) {
                    synchronized(s) {
                        try {
                            s.basicRemote.sendText(stats)
                        } catch (e: Exception) {
                            logger.error(e) { "Error sending stats to session ${s.id}" }
                        }
                    }
                }
            }
        }

        fun incrementMessages() {
            messagesSent.incrementAndGet()
            broadcastStats()
        }

        fun incrementSessions() {
            connectedUsers.incrementAndGet()
            broadcastStats()
        }

        fun decrementSessions() {
            connectedUsers.decrementAndGet()
            broadcastStats()
        }
    }

    @OnOpen
    fun onOpen(session: Session) {
        sessions.add(session)
        broadcastStats()
    }

    @OnError
    fun onError(
        session: Session,
        errorReason: Throwable,
    ) {
        logger.error(errorReason) { "Session ${session.id} closed because of ${errorReason.javaClass.name}" }
    }
}

@ServerEndpoint("/eliza")
@Component
class ElizaEndpoint {
    private val eliza = Eliza()

    companion object {
        private val sessions = ConcurrentHashMap.newKeySet<Session>()
    }

    /**
     * Successful connection
     *
     * @param session
     */
    @OnOpen
    fun onOpen(session: Session) {
        logger.info { "Server Connected ... Session ${session.id}" }
        with(session.basicRemote) {
            sendTextSafe("The doctor is in.")
            sendTextSafe("What's on your mind?")
            sendTextSafe("---")
        }
        sessions.add(session)
        StatsEndpoint.incrementSessions()
    }

    /**
     * Connection closure
     *
     * @param session
     */
    @OnClose
    fun onClose(
        session: Session,
        closeReason: CloseReason,
    ) {
        logger.info { "Session ${session.id} closed because of $closeReason" }
        sessions.remove(session)
        StatsEndpoint.decrementSessions()
    }

    /**
     * Message received
     *
     * @param message
     */
    @OnMessage
    fun onMsg(
        message: String,
        session: Session,
    ) {
        logger.info { "Server Message ... Session ${session.id}" }
        val currentLine = Scanner(message.lowercase(Locale.getDefault()))
        if (currentLine.findInLine("bye") == null) {
            logger.info { "Server received \"${message}\" from Session ${session.id}" }
            runCatching {
                for (s in sessions) {
                    if (session.isOpen) {
                        val line = Scanner(message.lowercase(Locale.getDefault()))
                        with(s.basicRemote) {
                            val response = eliza.respond(line)
                            StatsEndpoint.incrementMessages()
                            sendTextSafe(response)
                            logger.info { "Server sent \"${response}\" to Session ${s.id}" }
                            sendTextSafe("---")
                            logger.info { "Server sent --- to Session ${s.id}" }
                        }
                    }
                }
            }.onFailure {
                logger.error(it) { "Error while sending message" }
                session.close(CloseReason(CloseCodes.CLOSED_ABNORMALLY, "I'm sorry, I didn't understand that."))
            }
        } else {
            session.close(CloseReason(CloseCodes.NORMAL_CLOSURE, "Alright then, goodbye!"))
        }
    }

    @OnError
    fun onError(
        session: Session,
        errorReason: Throwable,
    ) {
        logger.error(errorReason) { "Session ${session.id} closed because of ${errorReason.javaClass.name}" }
    }
}
