package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gson.Gson;

public class AuditLogEvent {
    String http_method;
    Integer http_status;
    Long when;

    public AuditLogEvent(String http_method, Integer http_status, Long when) {
        this.when = when;
        this.http_method = http_method;
        this.http_status = http_status;
    }

    public String serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
