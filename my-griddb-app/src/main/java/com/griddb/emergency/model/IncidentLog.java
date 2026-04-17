package com.griddb.emergency.model;

public class IncidentLog {
    private java.util.Date timestamp; // Converted for GridDB generic Date mapping via older API or specific timestamp

    private String incidentId;
    private String incidentType;
    private java.util.Date reportedTime;
    private Integer responseDelaySeconds;
    private Double latitude;
    private Double longitude;

    public IncidentLog() {
    }

    public IncidentLog(java.util.Date timestamp, String incidentId, String incidentType, java.util.Date reportedTime, Integer responseDelaySeconds, Double latitude, Double longitude) {
        this.timestamp = timestamp;
        this.incidentId = incidentId;
        this.incidentType = incidentType;
        this.reportedTime = reportedTime;
        this.responseDelaySeconds = responseDelaySeconds;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public java.util.Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(java.util.Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public java.util.Date getReportedTime() {
        return reportedTime;
    }

    public void setReportedTime(java.util.Date reportedTime) {
        this.reportedTime = reportedTime;
    }

    public Integer getResponseDelaySeconds() {
        return responseDelaySeconds;
    }

    public void setResponseDelaySeconds(Integer responseDelaySeconds) {
        this.responseDelaySeconds = responseDelaySeconds;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
