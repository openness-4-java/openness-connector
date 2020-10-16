package it.unimore.dipi.iot.openness.connector;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * @author Stefano Mariani, Ph.D. - stefano.mariani@unimore.it
 * @project openness-connector
 * @created 16/10/2020 - 9:11
 */
@WebSocket(maxTextMessageSize = 64 * 1024)  // TODO here or in implementing class?
public interface WebsocketHandle {

    boolean awaitClose(int duration, final TimeUnit unit) throws InterruptedException;

    @OnWebSocketConnect
    void onConnect(final Session session);

    @OnWebSocketMessage
    void onMessage(final String msg);

    @OnWebSocketError
    void onError(final Throwable cause);

    @OnWebSocketClose
    void onClose(int statusCode, final String reason);

}
