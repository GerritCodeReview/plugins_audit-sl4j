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

import static com.google.gerrit.audit.AuditEvent.UNKNOWN_SESSION_ID;

import com.google.gerrit.audit.HttpAuditEvent;
import com.google.gerrit.server.AccessPath;
import com.googlesource.gerrit.plugins.auditsl4j.AuditUser;
import com.googlesource.gerrit.plugins.auditsl4j.LoggerAudit;
import com.googlesource.gerrit.plugins.auditsl4j.TransformableAuditLogType;
import com.googlesource.gerrit.plugins.auditsl4j.TransformableLog;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPLog implements TransformableLog {
  private static final Logger log = LoggerFactory.getLogger(HTTPLog.class);
  private String ip;
  private String user;
  private String method;
  private String timestamp;
  private String resource;
  private String protocol;
  private Integer status;
  private String contentLength;
  private String referrer;
  private String userAgent;

  public HTTPLog(
      String ip,
      String user,
      String timestamp,
      String method,
      String resource,
      String protocol,
      Integer status,
      String contentLength,
      String referrer,
      String userAgent) {
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

  public static Optional<HTTPLog> createFromLog(String line) {
    // HTTP log example:
    // 104.32.164.100 - - [24/Jan/2019:00:00:03 +0000] "GET /plugins/events-log/ HTTP/1.1" 404 9 -
    // "Apache-HttpClient/4.5.3 (Java/1.8.0_191)"
    Matcher a =
        Pattern.compile(
                "^(?<ip>.*?)\\s-\\s(?<user>.*?)\\s\\["
                    + "(?<timestamp>.*?)\\]\\s\""
                    + "(?<method>\\w+)\\s(?<resource>.*?)\\s(?<protocol>.*?)\"\\s"
                    + "(?<status>\\d+)\\s(?<contentLength>\\d+|-)\\s(?<referrer>.*?)\\s(?<userAgent>.*?)$")
            .matcher(line);
    if (a.matches()) {
      try {
        return Optional.of(
            new HTTPLog(
                a.group("ip"),
                a.group("user"),
                a.group("timestamp"),
                a.group("method"),
                a.group("resource"),
                a.group("protocol"),
                Integer.parseInt(a.group("status")),
                a.group("contentLength"),
                a.group("referrer"),
                a.group("userAgent")));
      } catch (Exception e) {
        log.error("Something wrong while parsing line: " + line);
      }
    } else {
      log.error("Can't extract any info from line: " + line);
    }
    return Optional.empty();
  }

  private AuditUser getAuditUser() {
    AuditUser au = new AuditUser();
    au.setUserName(this.user);
    au.setAccessPath(getAccessPath());
    return au;
  }

  private AccessPath getAccessPath() {
    Matcher a = Pattern.compile("^\"git.*?").matcher(this.userAgent);
    return a.matches() ? AccessPath.GIT : AccessPath.REST_API;
  }

  public Optional<Long> getWhen() {
    SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z");
    try {
      return Optional.of(format.parse(this.timestamp).getTime());
    } catch (ParseException pe) {
      log.error(
          "Can't parse timestamp: '" + this.timestamp + "'. Error message: " + pe.getMessage());
    }
    return Optional.empty();
  }

  public Optional<String> toAuditLog(LoggerAudit loggerAudit) {
    return getWhen()
        .map(
            when ->
                new HttpAuditEvent(
                    UNKNOWN_SESSION_ID,
                    this.getAuditUser(),
                    this.resource,
                    when,
                    null,
                    this.method,
                    null,
                    this.status,
                    null))
        .map(
            httpAuditEvent -> {
              AuditUser au = getAuditUser();
              if (au.getAccessPath() == AccessPath.REST_API) {
                return loggerAudit.getAuditString(
                    httpAuditEvent, TransformableAuditLogType.ExtendedHttpAuditEvent);
              } else {
                return loggerAudit.getAuditString(
                    httpAuditEvent, TransformableAuditLogType.HttpAuditEvent);
              }
            });
  }

  public static String logFilenameBase() {
    return "httpd_log";
  }

  public String getIp() {
    return ip;
  }

  public String getUser() {
    return user;
  }

  public String getMethod() {
    return method;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getResource() {
    return resource;
  }

  public String getProtocol() {
    return protocol;
  }

  public Integer getStatus() {
    return status;
  }

  public String getContentLength() {
    return contentLength;
  }

  public String getReferrer() {
    return referrer;
  }

  public String getUserAgent() {
    return userAgent;
  }
}
