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
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@Service
public class OpenDataIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(OpenDataIngestionService.class);
    private static final String SOCRATA_API_URL = "https://data.seattle.gov/resource/kzjm-xkqj.json?$limit=50";

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
                String incidentType = obj.optString("type", "UNKNOWN");
                String reportedTimeStr = obj.optString("datetime");

                if (reportedTimeStr.isEmpty()) continue;

                LocalDateTime reportedTime = LocalDateTime.parse(reportedTimeStr, FORMATTER);

                // Simulation: Random delay gap for demonstration
                long delaySeconds = (long) (Math.random() * 840) + 120;
                LocalDateTime arrivalTime = reportedTime.plusSeconds(delaySeconds);

                double lat = obj.has("latitude") ? obj.getDouble("latitude") : 47.6062;
                double lon = obj.has("longitude") ? obj.getDouble("longitude") : -122.3321;

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
            logger.info("Fetched {} live incidents from Socrata", logs.size());

        } catch (Exception e) {
            logger.error("Error communicating with open data", e);
        }
        return logs;
    }
}
