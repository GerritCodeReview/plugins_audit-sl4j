package com.googlesource.gerrit.plugins.auditsl4j;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AuditHttpLogTest {

    @Test
    public void succesullyParseHttpLog() {
        String ip = "104.32.164.100";
        String user = "myUser";
        String method = "GET";
        String timestamp = "24/Jan/2019:00:00:03 +0000";
        String resource = "/plugins/events-log/";
        String protocol = "HTTP/1.1";
        Integer status = 404;
        Integer contentLenght = 9;
        String referrer = "ciccio";
        String userAgent = "\"Apache-HttpClient/4.5.3 (Java/1.8.0_191)\"";
        // 104.32.164.100 - - [24/Jan/2019:00:00:03 +0000] "GET /plugins/events-log/ HTTP/1.1" 404 9 - "Apache-HttpClient/4.5.3 (Java/1.8.0_191)"
        String logLine = String.format("%s - %s [%s] \"%s %s %s\" %d %d %s %s", ip, user, timestamp, method, resource, protocol, status, contentLenght, referrer, userAgent);

        AuditHTTPLog expected = new AuditHTTPLog(ip, user, timestamp, method, resource, protocol, status, contentLenght, referrer, userAgent);
        AuditHTTPLog got = AuditHTTPLog.parseHTTPLogs(logLine);
        assertEquals(expected.ip, got.ip);
        assertEquals(expected.user, got.user);
        assertEquals(expected.method, got.method);
        assertEquals(expected.timestamp, got.timestamp);
        assertEquals(expected.resource, got.resource);
        assertEquals(expected.referrer, got.referrer);
        assertEquals(expected.protocol, got.protocol);
        assertEquals(expected.status, got.status);
        assertEquals(expected.contentLength, got.contentLength);
        assertEquals(expected.userAgent, got.userAgent);
    }

    @Test
    public void invalidHttpLog() {
        assertNull(AuditHTTPLog.parseHTTPLogs("invalid http log"));
    }
}
