package com.griddb.emergency.service;

import com.griddb.emergency.model.IncidentLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@Service
public class OpenDataIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(OpenDataIngestionService.class);
    private static final String SOCRATA_API_URL = "https://data.sfgov.org/resource/nuek-vuh3.json?$where=on_scene_dttm%20IS%20NOT%20NULL&$limit=50&$order=received_dttm%20DESC";

    // Format matches Socrata's floating timestamp exactly
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public List<IncidentLog> fetchLiveIncidents() {
        List<IncidentLog> logs = new ArrayList<>();
        try {
            URL url = new URL(SOCRATA_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            JSONArray jsonArray = new JSONArray(response.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                String incidentId = obj.optString("incident_number", "UNKNOWN");
                String incidentType = obj.optString("call_type", "UNKNOWN");
                String reportedTimeStr = obj.optString("received_dttm");
                String arrivalTimeStr = obj.optString("on_scene_dttm");

                if (reportedTimeStr.isEmpty() || arrivalTimeStr.isEmpty()) continue;

                LocalDateTime reportedTime = LocalDateTime.parse(reportedTimeStr, FORMATTER);
                LocalDateTime arrivalTime = LocalDateTime.parse(arrivalTimeStr, FORMATTER);

                long delaySeconds = ChronoUnit.SECONDS.between(reportedTime, arrivalTime);
                if (delaySeconds < 0) delaySeconds = 0; // Sanity check for negative delays

                double lat = 37.7749; // Default SF lat
                double lon = -122.4194; // Default SF lon
                
                if (obj.has("case_location")) {
                    JSONObject location = obj.getJSONObject("case_location");
                    if (location.has("coordinates")) {
                        JSONArray coords = location.getJSONArray("coordinates");
                        if (coords.length() == 2) {
                            lon = coords.getDouble(0);
                            lat = coords.getDouble(1);
                        }
                    }
                }

                IncidentLog log = new IncidentLog(
                        Date.from(arrivalTime.atZone(ZoneId.systemDefault()).toInstant()),
                        incidentId,
                        incidentType,
                        Date.from(reportedTime.atZone(ZoneId.systemDefault()).toInstant()),
                        (int) delaySeconds,
                        lat,
                        lon
                );
                logs.add(log);
            }
            logger.info("Fetched {} live incidents from SF Socrata", logs.size());

        } catch (Exception e) {
            logger.error("Error communicating with open data", e);
        }
        return logs;
    }
}
