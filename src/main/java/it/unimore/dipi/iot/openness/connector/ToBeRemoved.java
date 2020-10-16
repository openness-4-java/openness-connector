package it.unimore.dipi.iot.openness.connector;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class ToBeRemoved {

    public static void main(String[] args) {
        SslContextFactory ssl = new SslContextFactory.Client(true);
        ssl.setKeyStorePath("certs/5b5c0eaec10b708888c225e366f2f1239ea0541e4d687d644cfcd98c26fc312b.client.p12");
        ssl.setTrustStorePath("certs/5b5c0eaec10b708888c225e366f2f1239ea0541e4d687d644cfcd98c26fc312b.ca.jks");
        ssl.setKeyStorePassword("changeit");
        HttpClient http = new HttpClient(ssl);
        WebSocketClient client = new WebSocketClient(http);
        NotificationsHandle socket = new NotificationsHandle();
        try
        {
            client.start();

            URI echoUri = new URI("wss://eaa.openness:7443/notifications");
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Host", "testing:OpenNessConnectorTester");
            client.connect(socket, echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);

            // wait for closed socket connection.
            socket.awaitClose(300, TimeUnit.SECONDS);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        finally
        {
            try
            {
                client.stop();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

}
