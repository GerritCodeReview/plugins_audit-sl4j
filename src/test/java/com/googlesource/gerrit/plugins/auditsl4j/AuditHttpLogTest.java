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
        String contentLenght = "9";
        String referrer = "ciccio";
        String userAgent = "\"Apache-HttpClient/4.5.3 (Java/1.8.0_191)\"";
        // 104.32.164.100 - - [24/Jan/2019:00:00:03 +0000] "GET /plugins/events-log/ HTTP/1.1" 404 9 - "Apache-HttpClient/4.5.3 (Java/1.8.0_191)"
        String logLine = String.format("%s - %s [%s] \"%s %s %s\" %d %s %s %s", ip, user, timestamp, method, resource, protocol, status, contentLenght, referrer, userAgent);

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
    public void handleIPV6ParseHttpLog() {
        String logLine =
            "2405:204:a313:675::17a:b0b1 - - [19/Jan/2019:18:15:02 +0000] \"GET /plugins/codemirror-editor/static/codemirror_editor.js HTTP/1.1\" 200 1498 \"https://gerrithub.io/plugins/codemirror-editor/static/codemirror_editor.html\" \"Mozilla/5.0 (Linux; Android 4.2.1; en-us; Nexus 5 Build/JOP40D) AppleWebKit/535.19 (KHTML, like Gecko; googleweblight) Chrome/38.0.1025.166 Mobile Safari/535.19\"";
        AuditHTTPLog auditHTTPLog = AuditHTTPLog.parseHTTPLogs(logLine);
        assertEquals("2405:204:a313:675::17a:b0b1", auditHTTPLog.ip);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void handleNonNumericalContentLengthParseHttpLog() {
        String logLine = "171.13.14.52 - - [19/Jan/2019:00:00:46 +0000] \"HEAD /Documentation/index.html HTTP/1.1\" 200 - - \"Mozilla/5.0 (Windows NT 10.0 WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36\"";
        AuditHTTPLog.parseHTTPLogs(logLine);
    }

    @Test
    public void invalidHttpLog() {
        assertNull(AuditHTTPLog.parseHTTPLogs("invalid http log"));
    }
}
