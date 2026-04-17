package com.griddb.emergency.controller;

import com.griddb.emergency.model.IncidentLog;
import com.griddb.emergency.service.GridDBPersistenceService;
import com.griddb.emergency.service.OpenDataIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private OpenDataIngestionService ingestionService;

    @Autowired
    private GridDBPersistenceService dbService;

    @GetMapping("/")
    public String dashboard(Model model) {
        return "dashboard"; // Serves dashboard.html
    }

    // Explicitly triggers data collection from Socrata to insert into GridDB
    @PostMapping("/api/sync")
    @ResponseBody
    public ResponseEntity<String> syncData() {
        List<IncidentLog> logs = ingestionService.fetchLiveIncidents();
        dbService.persistLogs(logs);
        return ResponseEntity.ok("{\"status\": \"Synched " + logs.size() + " records to GridDB\"}");
    }

    // Fetch the recent time-series list mapped safely to JSON for charting
    @GetMapping("/api/data")
    @ResponseBody
    public ResponseEntity<List<IncidentLog>> getRecentData() {
        // Will return empty if GridDB uninitialized, gracefully handled by UI
        List<IncidentLog> recent = dbService.fetchRecentLogs();
        
        // Setup mock if gridDB is disconnected so UI testing works implicitly
        if (recent.isEmpty()) {
             recent = ingestionService.fetchLiveIncidents(); 
        }
        
        return ResponseEntity.ok(recent);
    }
}
