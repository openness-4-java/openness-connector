package it.unimore.dipi.iot.openness.notification;

import it.unimore.dipi.iot.openness.process.MyNotificationsHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 16/10/2020 - 9:11
 */
//@WebSocket(maxTextMessageSize = 64 * 1024)
public abstract class AbstractWebSocketHandler extends WebSocketAdapter {

    protected static final Logger logger = LoggerFactory.getLogger(MyNotificationsHandler.class);
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    protected Session session;

    public boolean awaitClose(int duration, final TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @Override
    public void onWebSocketConnect(Session session) {

        if(session != null) {
            this.session = session;
            logger.info("Connection to {} open: {} (secured:{})", this.session.getRemoteAddress(), this.session.isOpen(), this.session.isSecure());
        }
        else
            logger.error("Error ! Session = null !");
    }

    @Override
    public void onWebSocketText(String message) {
        logger.info("Message got: {}", message);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        logger.info("Connection error: {} -> {}", cause.getCause(), cause.getLocalizedMessage());
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.info("Connection closed: {} -> {}", statusCode, reason);
        this.closeLatch.countDown(); // trigger latch
    }

}
