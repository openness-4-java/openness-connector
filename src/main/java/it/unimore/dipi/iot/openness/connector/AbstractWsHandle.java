package it.unimore.dipi.iot.openness.connector;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 16/10/2020 - 9:11
 */
public abstract class AbstractWsHandle implements WebsocketHandle {

    protected static final Logger logger = LoggerFactory.getLogger(NotificationsHandle.class);
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    protected Session session;

    @Override
    public boolean awaitClose(int duration, final TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @Override
    public void onConnect(Session session) {
        logger.info("Connection to {} open: {} (secured:{})", session.getRemoteAddress(), session.isOpen(), session.isSecure());
        this.session = session;
    }

    @Override
    public void onError(Throwable cause) {
        logger.error("Connection error: {} -> {}", cause.getCause(), cause.getLocalizedMessage());
        cause.printStackTrace(System.out);
    }

    @Override
    public void onClose(int statusCode, String reason) {
        logger.info("Connection closed: {} -> {}", statusCode, reason);
        this.closeLatch.countDown(); // trigger latch
    }

}
