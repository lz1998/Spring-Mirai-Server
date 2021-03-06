package net.lz1998.mirai.handler

import com.google.protobuf.util.JsonFormat
import net.lz1998.mirai.bot.ApiSender
import net.lz1998.mirai.bot.BotFactory
import net.lz1998.mirai.bot.CoolQ
import onebot.OnebotFrame
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.AbstractWebSocketHandler
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class WebSocketHandler : TextWebSocketHandler() {
    val botMap = mutableMapOf<Long, CoolQ>()

    @Autowired
    lateinit var botFactory: BotFactory

    val sessionMap = mutableMapOf<Long, WebSocketSession>()

    val jsonFormatParser: JsonFormat.Parser = JsonFormat.parser().ignoringUnknownFields()

    @Autowired
    lateinit var frameHandler: FrameHandler

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val xSelfId = session.handshakeHeaders["x-self-id"]?.get(0)?.toLong() ?: 0L
        if (xSelfId == 0L) {
            session.close()
            return
        }
        sessionMap[xSelfId] = session
        println("$xSelfId connected")
        botMap[xSelfId] = botFactory.createBot(xSelfId, session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val xSelfId = session.handshakeHeaders["x-self-id"]?.get(0)?.toLong() ?: 0L
        if (xSelfId == 0L) {
            return
        }
        sessionMap.remove(xSelfId, session)
        println("$xSelfId disconnected")
        botMap.remove(xSelfId)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val frameBuilder = OnebotFrame.Frame.newBuilder()
        jsonFormatParser.merge(message.payload, frameBuilder)
        val frame = frameBuilder.build()
        session.sendMessage(PingMessage())
        Thread {
            frameHandler.handleFrame(frame)
        }.start()

    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val frame = OnebotFrame.Frame.parseFrom(message.payload)
        session.sendMessage(PingMessage())
        Thread {
            frameHandler.handleFrame(frame)
        }.start()
    }


}