package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gson.Gson;

import java.io.Serializable;

public class AuditLog implements Serializable {
    String type;
    String http_method;

    public AuditLog(String type, String http_method) {
        this.type = type;
        this.http_method = http_method;
    }

    public String serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
