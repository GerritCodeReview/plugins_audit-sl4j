/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.auditsl4j.logsource;

import com.googlesource.gerrit.plugins.auditsl4j.AuditRendererToJson;
import com.googlesource.gerrit.plugins.auditsl4j.AuditWriterToStringList;
import com.googlesource.gerrit.plugins.auditsl4j.LoggerAudit;
import java.util.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpLogTest {

  LoggerAudit loggerAudit =
      new LoggerAudit(new AuditWriterToStringList(), new AuditRendererToJson());

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
    // 104.32.164.100 - - [24/Jan/2019:00:00:03 +0000] "GET /plugins/events-log/ HTTP/1.1" 404 9 -
    // "Apache-HttpClient/4.5.3 (Java/1.8.0_191)"
    String logLine =
        String.format(
            "%s - %s [%s] \"%s %s %s\" %d %s %s %s",
            ip,
            user,
            timestamp,
            method,
            resource,
            protocol,
            status,
            contentLenght,
            referrer,
            userAgent);

    HTTPLog expected =
        new HTTPLog(
            ip,
            user,
            timestamp,
            method,
            resource,
            protocol,
            status,
            contentLenght,
            referrer,
            userAgent);
    Optional<HTTPLog> maybeHTTPLog = HTTPLog.createFromLog(logLine);
    assertTrue("Didn't create HTTPLog", maybeHTTPLog.isPresent());
    HTTPLog gotHTTPLog = maybeHTTPLog.get();
    assertEquals(expected.getIp(), gotHTTPLog.getIp());
    assertEquals(expected.getUser(), gotHTTPLog.getUser());
    assertEquals(expected.getMethod(), gotHTTPLog.getMethod());
    assertEquals(expected.getTimestamp(), gotHTTPLog.getTimestamp());
    assertEquals(expected.getResource(), gotHTTPLog.getResource());
    assertEquals(expected.getReferrer(), gotHTTPLog.getReferrer());
    assertEquals(expected.getProtocol(), gotHTTPLog.getProtocol());
    assertEquals(expected.getStatus(), gotHTTPLog.getStatus());
    assertEquals(expected.getContentLength(), gotHTTPLog.getContentLength());
    assertEquals(expected.getUserAgent(), gotHTTPLog.getUserAgent());
  }

  @Test
  public void handleIPV6ParseHttpLog() {
    String logLine =
        "2405:204:a313:675::17a:b0b1 - - [19/Jan/2019:18:15:02 +0000] \"GET /plugins/codemirror-editor/static/codemirror_editor.js HTTP/1.1\" 200 1498 \"https://gerrithub.io/plugins/codemirror-editor/static/codemirror_editor.html\" \"Mozilla/5.0 (Linux; Android 4.2.1; en-us; Nexus 5 Build/JOP40D) AppleWebKit/535.19 (KHTML, like Gecko; googleweblight) Chrome/38.0.1025.166 Mobile Safari/535.19\"";
    Optional<HTTPLog> maybeHTTPLog = HTTPLog.createFromLog(logLine);
    assertTrue("Didn't create HTTPLog", maybeHTTPLog.isPresent());
    assertEquals("2405:204:a313:675::17a:b0b1", maybeHTTPLog.get().getIp());
  }

  @Test(expected = Test.None.class /* no exception expected */)
  public void handleNonNumericalContentLengthParseHttpLog() {
    String logLine =
        "171.13.14.52 - - [19/Jan/2019:00:00:46 +0000] \"HEAD /Documentation/index.html HTTP/1.1\" 200 - - \"Mozilla/5.0 (Windows NT 10.0 WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36\"";
    HTTPLog.createFromLog(logLine);
  }

  @Test
  public void invalidHttpLog() {
    assertEquals(Optional.empty(), HTTPLog.createFromLog("invalid http log"));
  }

  @Test(expected = Test.None.class /* no exception expected */)
  public void correctlyTransform_HttpAuditEvent() throws Exception {
    HTTPLog auditHTTPLog =
        new HTTPLog(
            "10.10.100.108",
            "anyUser",
            "19/Jan/2019:00:00:00 +0000",
            "amnyResource",
            "anyProtocol",
            "-",
            200,
            "-",
            "-",
            "\"git/1.8.3.1\"");

    Optional<String> maybeAuditLog = auditHTTPLog.toAuditLog(loggerAudit);
    assertTrue("Didn't Audit Log", maybeAuditLog.isPresent());

    String auditLog = maybeAuditLog.get();
    assertTrue(auditLog.contains("\"type\":\"HttpAuditEvent\""));
    assertTrue(auditLog.contains("\"access_path\":\"GIT\""));
  }

  @Test(expected = Test.None.class /* no exception expected */)
  public void correctlyTransform_ExtendedHttpAuditEvent() throws Exception {
    HTTPLog auditHTTPLog =
        new HTTPLog(
            "10.10.100.108",
            "anyUser",
            "19/Jan/2019:00:00:00 +0000",
            "amnyResource",
            "anyProtocol",
            "-",
            200,
            "-",
            "-",
            "\"anyUserAgent\"");

    Optional<String> maybeAuditLog = auditHTTPLog.toAuditLog(loggerAudit);
    assertTrue("Didn't Audit Log", maybeAuditLog.isPresent());

    String auditLog = maybeAuditLog.get();
    assertTrue(auditLog.contains("\"type\":\"ExtendedHttpAuditEvent\""));
    assertTrue(auditLog.contains("\"access_path\":\"REST_API\""));
  }
}
