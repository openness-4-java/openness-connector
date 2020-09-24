package it.unimore.dipi.iot.openness.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 24/09/2020 - 12:51
 */
public class EdgeApplicationAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(EdgeApplicationAuthenticator.class);

    private static final String OPENNESS_CONTROLLER_BASE_URL = "http://127.0.0.1:7080/auth";

    private OkHttpClient httpClient = null;

    private ObjectMapper objectMapper = null;

    public EdgeApplicationAuthenticator(){
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void authenticateApplication(String applicationId){

        try{

            if(httpClient == null){
                logger.error("HTTP Client = NULL !");
                return;
            }

            String queryString = "";

            Request request = new Request.Builder()
                    .url(OPENNESS_CONTROLLER_BASE_URL + queryString)
                    .build();

            Call call = this.httpClient.newCall(request);
            Response response = call.execute();

            //TODO Check response body != NULL
            if(response != null){

                logger.info("Application Authentication Response Code: {}", response.code());

                /*
                String body = response.body().string();

                logger.info("Received Response Code: {} -> Body: {}", response.code(), body);

                DataStoreResponse<DataStoreWeatherStationResult> dataStoreResult = new ObjectMapper()
                        .readerFor(new TypeReference<DataStoreResponse<DataStoreWeatherStationResult>>() {})
                        .readValue(body);

                dataStoreResult.getResult().getWeatherStationList().forEach(weatherStationRecord -> logger.info("Weather Station: {}", weatherStationRecord));
                */
            }
            else
                logger.error("NULL Response Received for request: {}", request);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
