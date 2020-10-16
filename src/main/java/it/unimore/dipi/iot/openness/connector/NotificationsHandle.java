package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.dto.service.NotificationToConsumer;
import it.unimore.dipi.iot.openness.dto.service.TerminateNotification;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebSocket(maxTextMessageSize = 64 * 1024)  // TODO what?
public class NotificationsHandle {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsHandle.class);
    private final CountDownLatch closeLatch;
    private Session session;
    private ObjectMapper objectMapper;

    public NotificationsHandle() {
        this.closeLatch = new CountDownLatch(1);
        this.objectMapper = new ObjectMapper();
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connection to {} open: {} (secured:{})", session.getRemoteAddress(), session.isOpen(), session.isSecure());
        this.session = session;
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        logger.info("Message got: {}", msg);
        try {
            final NotificationToConsumer shutdown = this.objectMapper.readValue(msg, NotificationToConsumer.class);
            final TerminateNotification tn = new TerminateNotification();
            if (shutdown.getName().equals(tn.getName()) && shutdown.getVersion().equals(tn.getVersion()) && shutdown.getPayload().getPayload().equals(tn.getPayload().getPayload())) {
                logger.info("Received notifications termination request, closing web socket...");
                this.session.close();
                this.session.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        logger.error("Connection error: {} -> {}", cause.getCause(), cause.getLocalizedMessage());
        cause.printStackTrace(System.out);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.info("Connection closed: {} -> {}", statusCode, reason);
        this.closeLatch.countDown(); // trigger latch
    }

}
