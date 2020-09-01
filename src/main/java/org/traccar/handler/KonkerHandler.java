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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.model.Position;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@ChannelHandler.Sharable
public class KonkerHandler extends BaseDataHandler {

    private static String getEnv(String variable, String defaultValue) {
        String value = System.getenv(variable);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private static final String THIRD_PARTY_PROCESSOR = "TEST";
    private static final String KONKER_URL = KonkerHandler.getEnv("KONKER_URL", "https://data.prod.konkerlabs.net/gateway/data/pub");
    private static final String KONKER_AUTH = KonkerHandler.getEnv("KONKER_AUTH", "Bearer <GATEWAY TOKEN>");

    private static final Logger LOGGER = LoggerFactory.getLogger(KonkerHandler.class);

    public KonkerHandler() {
        LOGGER.info("KONKER INTEGRATION");
        LOGGER.info("sending data to {}", KONKER_URL);
        LOGGER.info("using credentials {}", KONKER_AUTH);
        // TODO: if credentials are available, request data from the user account using Konker API
        //       to show which account / tenant are been used to send data to ...
        //       and test KonkerAPI usage with this credential ...
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

            LOGGER.debug("NEW POSITION = {}", position);
            LOGGER.debug("DEVICE ID = {}", position.getDeviceId());

            sendToPlatform(position);

        } catch (Exception error) {
            LOGGER.warn("Failed to send position to KONKER PLATFORM", error);
        }

        return position;
    }

    private void sendToPlatform(Position position) {
        try {
            // augument positin with IMEI from device record ...
            String uniqueId = Context.getIdentityManager().getById(position.getDeviceId()).getUniqueId();
            position.set("imei", uniqueId);

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

            String imei = (String) position.getAttributes().get("imei");

            if (imei.length() < 12) {
                imei = StringUtils.leftPad(imei, 12, '0');
            }

            data.put("imei", imei);
            data.put("channel", "location");
            data.put("_lat", position.getLatitude());
            data.put("_lon", position.getLongitude());
            // FIX: use position.getFixTime() to avoid problems when receiving events from the past ... 
            //      otherwise the old events will be injested as new one on the platform
            // 
            // data.put("_ts", position.getDeviceTime().getTime()); 
            data.put("_ts", position.getFixTime().getTime()); // use device fix time to send this data to the platform 
            data.put("protocol", position.getProtocol());
            data.put("serverTime", position.getServerTime().getTime());
            data.put("height", position.getAltitude());
            data.put("gpsSpeed", position.getSpeed());
            data.put("direction", position.getCourse());
            data.put("accuracy", position.getAccuracy());
            data.put("deviceId", position.getDeviceId());
            data.put("battery", position.getAttributes().get("batteryLevel"));
            data.put("distance", position.getAttributes().get("distance"));
            data.put("totalDistance", position.getAttributes().get("totalDistance"));
            data.put("gsmSignal", -1);
            data.put("gpsSignal", -1);
            data.put("attr", position.getAttributes());

            // sample
            // [{"_lat":-23.41832,"_lon":-46.76391666666667,"_ts":-1,"battery":45,"height":40,"gsmSignal":9,"gpsSpeed":46,"gpsSignal":10,"direction":150,"imei":"086728203271426
            //9","channel":"location"}]
            // http://data.demo.konkerlabs.net/gateway/data/pub

            String payload = gson.toJson(data);

            payload = String.format("[%s]", payload);

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

            LOGGER.debug("PAYLOAD = {}", payload);
            LOGGER.debug("OUTPUT => {}", content.toString());
            LOGGER.debug("status = {}", status);


        } catch (MalformedURLException e) {
            e.printStackTrace();
            LOGGER.error("ERROR URL {}", e);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("IO ERROR {}", e);
        }


    }

}
