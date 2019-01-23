package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gerrit.audit.HttpAuditEvent;
import com.google.gerrit.server.AccessPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.gerrit.audit.AuditEvent.UNKNOWN_SESSION_ID;

public class AuditHTTPLog implements TransformableLog {
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

    private AuditUser getAuditUser() {
        AuditUser au = new AuditUser();
        au.username = this.user;
        au.setAccessPath(getAccessPath());
        return au;
    }

    private AccessPath getAccessPath() {
        Matcher a =
                Pattern.compile("^\"git.*?").matcher(this.userAgent);
        return a.matches() ? AccessPath.GIT : AccessPath.REST_API;
    }

    public long getWhen() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z");
        return format.parse( this.timestamp ).getTime();
    }

    public String toAuditLog(LoggerAudit loggerAudit) throws ParseException {
        AuditUser au = getAuditUser();
        HttpAuditEvent httpAuditEvent = new HttpAuditEvent(UNKNOWN_SESSION_ID, this.getAuditUser(), this.resource, this.getWhen(), null, this.method, null, this.status, null);
        if(au.getAccessPath() == AccessPath.REST_API ) {
            return loggerAudit.getAuditString(httpAuditEvent, TransformableAuditLogType.ExtendedHttpAuditEvent);
        }
        else {
            return loggerAudit.getAuditString(httpAuditEvent, TransformableAuditLogType.HttpAuditEvent);
        }
    }
}