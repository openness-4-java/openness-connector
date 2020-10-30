package it.unimore.dipi.iot.openness.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.notification.AbstractWebSocketHandler;
import it.unimore.dipi.iot.openness.dto.service.NotificationToConsumer;
import it.unimore.dipi.iot.openness.dto.service.TerminateNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 07/10/2020 - 14:03
 */
public class MyNotificationsHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MyNotificationsHandler.class);
    private ObjectMapper objectMapper;

    public MyNotificationsHandler() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onWebSocketText(String msg) {

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

}
