package com.googlesource.gerrit.plugins.auditsl4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuditHTTPLog {
    private static final Logger log = LoggerFactory.getLogger(AuditHTTPLog.class);
    String ip;
    String user;
    String method;
    String timestamp;
    String resource;
    String protocol;
    Integer status;
    String contentLength; // It could be undefined
    String referrer;
    String userAgent;

    // Default constructor
    public AuditHTTPLog(String ip, String user, String timestamp, String method, String resource, String protocol, Integer status, String contentLength, String referrer, String userAgent)
    {
        this.ip = ip;
        this.user = user;
        this.method = method;
        this.timestamp = timestamp;
        this.resource = resource;
        this.protocol = protocol;
        this.status = status;
        this.contentLength = contentLength;
        this.referrer = referrer;
        this.userAgent = userAgent;
    }

    static public AuditHTTPLog parseHTTPLogs(String line) {
    // 104.32.164.100 - - [24/Jan/2019:00:00:03 +0000] "GET /plugins/events-log/ HTTP/1.1" 404 9 -
    // "Apache-HttpClient/4.5.3 (Java/1.8.0_191)"
    Matcher a =
        Pattern.compile(
                "^(.*?)\\s-\\s(.*?)\\s\\[(.*?)\\]\\s\"(\\w+)\\s(.*?)\\s(.*?)\"\\s(\\d+)\\s(\\d+|-)\\s(.*?)\\s(.*?)$")
            .matcher(line);
        if (a.matches()) {
            try {
                return new AuditHTTPLog(a.group(1), a.group(2), a.group(3), a.group(4), a.group(5), a.group(6), Integer.parseInt(a.group(7)), a.group(8), a.group(9), a.group(10));
            } catch (Exception e) {
                log.error("Something wrong while parsing line: " + line);
            }
        }
        else {
            log.error("Can't extract any info from line: " + line);
        }
        return null;
    }

}