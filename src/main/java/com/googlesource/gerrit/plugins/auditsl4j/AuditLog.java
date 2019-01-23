package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class AuditLog implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(AuditLog.class);
    String type;
    AuditLogEvent event;

    public AuditLog(String type, AuditLogEvent event) {
        this.type = type;
        this.event = event;
    }

    public String serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
