package websockets

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import java.util.Locale
import java.util.Scanner
import java.util.concurrent.atomic.AtomicInteger

@Controller
class MessagingController {
    private val eliza = Eliza()
    private val messagesSent = AtomicInteger(0)

    @MessageMapping("/chat")
    @SendTo("/topic/broadcast")
    fun processMessage(message: String): Response {
        val line = Scanner(message.lowercase(Locale.getDefault()))
        val response = eliza.respond(line)
        val total = messagesSent.incrementAndGet()
        return Response(response, total)
    }
}
