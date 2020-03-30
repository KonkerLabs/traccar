/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.handler;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.model.Position;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@ChannelHandler.Sharable
public class KonkerHandler extends BaseDataHandler {

    private static final String THIRD_PARTY_PROCESSOR = "TEST";
    private static final String KONKER_URL = "https://data-webhook.prod.konkerlabs.net/default/location";
    // private static final String KONKER_AUTH =
    //  "Bearer 4d65e9f5a9054078a1db5420c5d56a8e05ebe2df6dd64d4b8c5c2d76b559c9dc31d0fffc9c3046dfb7cc5363fc5c10cc";
    private static final String KONKER_AUTH = "Bearer 944dc0ad-415e-4fe2-ace2-91bc7d9f68da";


    private static final Logger LOGGER = LoggerFactory.getLogger(KonkerHandler.class);

    public KonkerHandler() {
        LOGGER.info("KONKER INTEGRATION");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);

        LOGGER.info("HANDLER ADDED {}", ctx);
    }

    @Override
    protected Position handlePosition(Position position) {

        try {
            // send data to data-

            LOGGER.info("NEW POSITION = {}", position);
            LOGGER.info("DEVICE ID = {}", position.getDeviceId());

            sendToPlatform(position);

        } catch (Exception error) {
            LOGGER.warn("Failed to send position to KONKER PLATFORM", error);
        }

        return position;
    }

    private void sendToPlatform(Position position) {
        try {
            URL url = new URL(KONKER_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setInstanceFollowRedirects(true);

            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("X-Konker-DeviceIdField", "imei");
            con.setRequestProperty("X-Konker-DeviceChannelField",  "channel");
            con.setRequestProperty("Authorization", KONKER_AUTH);

            Gson gson = new Gson();

            Map<String, Object> data = new HashMap<>();

            data.put("imei", position.getAttributes().get("imei"));
            data.put("channel", "position");
            data.put("lat", position.getLatitude());
            data.put("lon", position.getLongitude());
            data.put("ts", position.getDeviceTime().getTime());
            data.put("protocol", position.getProtocol());
            data.put("serverTime", position.getServerTime().getTime());
            data.put("altitude", position.getAltitude());
            data.put("speed", position.getSpeed());
            data.put("course", position.getCourse());
            data.put("accuracy", position.getAccuracy());
            data.put("deviceId", position.getDeviceId());
            data.put("batteryLevel", position.getAttributes().get("batteryLevel"));
            data.put("distance", position.getAttributes().get("distance"));
            data.put("totalDistance", position.getAttributes().get("totalDistance"));

            String payload = gson.toJson(data);

            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.write(payload.getBytes());
            out.flush();
            out.close();

            int status = con.getResponseCode();

            Reader streamReader = null;

            if (status > 299) {
                streamReader = new InputStreamReader(con.getErrorStream());
            } else {
                streamReader = new InputStreamReader(con.getInputStream());
            }
            BufferedReader in = new BufferedReader(streamReader);
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            con.disconnect();


            LOGGER.info("PAYLOAD = {}", payload);
            LOGGER.info("status = {}", status);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
