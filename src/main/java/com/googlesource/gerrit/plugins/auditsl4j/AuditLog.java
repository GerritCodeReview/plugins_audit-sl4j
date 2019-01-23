package com.googlesource.gerrit.plugins.auditsl4j;

import java.io.Serializable;

public class AuditLog implements Serializable {
    String type;
    String http_method;

    public AuditLog(String type, String http_method) {
        this.type = type;
        this.http_method = http_method;
    }

    public String serialize() {
        // TODO Do it with jackson
        return String.format("{\"type\": \"%s\", \"event\": {\"http_method\": \"%s\" } }", this.type, this.http_method);
    }

}
