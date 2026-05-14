package com.griddb.emergency.service;

import com.griddb.emergency.model.IncidentLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.TimeZone;

@Service
public class GridDBPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(GridDBPersistenceService.class);

    @Value("${griddb.rest.url}")
    private String gridDBRestUrl;

    @Value("${griddb.cloud.username}")
    private String username;

    @Value("${griddb.cloud.password}")
    private String password;

    private String getAuthHeader() {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
    
    // ISO 8601 formatter commonly expected by GridDB Cloud
    private SimpleDateFormat getFormatter() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    public void persistLogs(List<IncidentLog> incidents) {
        if (incidents.isEmpty()) return;

        try {
            URL url = new URL(gridDBRestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT"); // As per GridDB Cloud standard for inserting rows
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", getAuthHeader());
            conn.setDoOutput(true);

            JSONArray payload = new JSONArray();
            SimpleDateFormat sdf = getFormatter();

            for (IncidentLog log : incidents) {
                JSONArray row = new JSONArray();
                row.put(sdf.format(log.getTimestamp()));
                row.put(log.getIncidentId());
                row.put(log.getIncidentType());
                row.put(sdf.format(log.getReportedTime()));
                row.put(log.getResponseDelaySeconds());
                row.put(log.getLatitude());
                row.put(log.getLongitude());
                payload.put(row);
            }

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                logger.info("Persisted {} logs to GridDB Cloud via REST", incidents.size());
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                logger.error("Failed to persist data. HTTP {}", code);
                br.lines().forEach(logger::error);
            }
            conn.disconnect();
        } catch (Exception e) {
            logger.error("Error writing to GridDB Cloud", e);
        }
    }
    
    // Extractor that works cleanly with rest controller providing recent data via POST
    public List<IncidentLog> fetchRecentLogs() {
        List<IncidentLog> logs = new ArrayList<>();
        try {
            URL url = new URL(gridDBRestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // POST required for queries on rows endpoint
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", getAuthHeader());
            conn.setDoOutput(true);

            String requestBody = "{\"offset\": 0, \"limit\": 100}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Fallback to live API data if Cloud cannot be reached immediately
            if (conn.getResponseCode() != 200) {
                logger.warn("Could not fetch logs from GridDB Cloud. HTTP {}", conn.getResponseCode());
                return logs;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            JSONObject jsonResponse = new JSONObject(response.toString());
            if(!jsonResponse.has("rows")) return logs;
            
            JSONArray rows = jsonResponse.getJSONArray("rows");
            SimpleDateFormat sdf = getFormatter();

            for (int i = 0; i < rows.length(); i++) {
                JSONArray row = rows.getJSONArray(i);
                IncidentLog log = new IncidentLog();
                log.setTimestamp(sdf.parse(row.getString(0)));
                log.setIncidentId(row.getString(1));
                log.setIncidentType(row.getString(2));
                log.setReportedTime(sdf.parse(row.getString(3)));
                log.setResponseDelaySeconds(row.getInt(4));
                log.setLatitude(row.getDouble(5));
                log.setLongitude(row.getDouble(6));
                logs.add(log);
            }
        } catch (Exception e) {
             logger.error("Error fetching GridDB logs via REST", e);
        }
        return logs;
    }
}
