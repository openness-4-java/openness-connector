package it.unimore.dipi.iot.openness.connector;

import it.unimore.dipi.iot.openness.dto.service.NotificationToSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.*;

import java.lang.reflect.Type;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 08/10/2020 - 10:03
 */
public class NotificationsStompSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsStompSessionHandler.class);

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        logger.info("New session established : " + session.getSessionId());
        session.subscribe("/notifications", this);
        logger.info("Subscribed to /notifications");
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error("Got an exception", exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return NotificationToSubscriber.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        NotificationToSubscriber msg = (NotificationToSubscriber) payload;
        logger.info("Received : " + msg.getPayload() + " from : " + msg.getUrn());
    }

}
