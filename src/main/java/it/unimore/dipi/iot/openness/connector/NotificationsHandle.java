package it.unimore.dipi.iot.openness.connector;

import it.unimore.dipi.iot.openness.dto.service.NotificationToSubscriber;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebSocket(maxTextMessageSize = 64 * 1024)  // TODO what?
public class NotificationsHandle {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsHandle.class);
    private final CountDownLatch closeLatch;

    public NotificationsHandle() {
        this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connection to {} open: {} (secured:{})", session.getRemoteAddress(), session.isOpen(), session.isSecure());
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        logger.info("Message got: {}", msg);
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        logger.error("Connection error: {} -> {}", cause.getCause(), cause.getLocalizedMessage());
        cause.printStackTrace(System.out);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.info("Connection closed: %d - %s%n", statusCode, reason);
        this.closeLatch.countDown(); // trigger latch
    }

}
