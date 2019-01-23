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

import com.google.gerrit.server.AccessPath;
import com.google.gerrit.server.audit.SshAuditEvent;
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

public class SSHLog implements TransformableLog {
  private static final Logger log = LoggerFactory.getLogger(SSHLog.class);

  private String user;
  private String session;
  private String timestamp;
  private String accountId;
  private String command;
  private String waitTime;
  private String execTime;
  private String result;

  public SSHLog(
      String timestamp,
      String session,
      String user,
      String accountId,
      String command,
      String waitTime,
      String execTime,
      String result) {
    this.user = user;
    this.session = session;
    this.timestamp = timestamp;
    this.accountId = accountId;
    this.command = command;
    this.waitTime = waitTime;
    this.execTime = execTime;
    this.result = result;
  }

  public static Optional<SSHLog> createFromLog(String line) {
    Matcher authCommand =
        Pattern.compile(
                "^\\[(?<timestamp>.*?)\\]\\s(?<session>.*?)\\s"
                    + "(?<user>.*?)\\s(?<accountId>.*?)\\s(?<command>LOGOUT|LOGIN)(:?\\sFROM.*?)?$")
            .matcher(line);

    Matcher nonAuthCommand =
        Pattern.compile(
                "^\\[(?<timestamp>.*?)\\]\\s(?<session>.*?)\\s(?<user>.*?)\\s(?<accountId>.*?)\\s(?<command>.*?)\\s(?<waitTime>\\d+ms)\\s(?<execTime>\\d+ms)\\s(?<result>.*?)$")
            .matcher(line);

    if (authCommand.matches()) {
      try {
        return Optional.of(
            new SSHLog(
                authCommand.group("timestamp"),
                authCommand.group("session"),
                authCommand.group("user"),
                authCommand.group("accountId"),
                authCommand.group("command"),
                null,
                null,
                "0"));
      } catch (Exception e) {
        log.error("Auth command match, but something wrong while parsing line: " + line);
      }
    } else if (nonAuthCommand.matches()) {
      try {
        return Optional.of(
            new SSHLog(
                nonAuthCommand.group("timestamp"),
                nonAuthCommand.group("session"),
                nonAuthCommand.group("user"),
                nonAuthCommand.group("accountId"),
                nonAuthCommand.group("command"),
                nonAuthCommand.group("waitTime"),
                nonAuthCommand.group("execTime"),
                nonAuthCommand.group("result")));
      } catch (Exception e) {
        log.error("Non Auth command match, but something wrong while parsing line: " + line);
      }
    } else {
      log.error("Can't extract any info from line: " + line);
    }
    return Optional.empty();
  }

  private AuditUser getAuditUser() {
    AuditUser au = new AuditUser();
    au.setUserName(this.user);
    au.setAccessPath(AccessPath.SSH_COMMAND);
    return au;
  }

  private Optional<Long> getWhen() {
    // Timestamp format example: 2019-01-23 12:44:04,723 +0100
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS Z");
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
                new SshAuditEvent(
                    this.session, getAuditUser(), this.command, when, null, this.result))
        .map(
            sshAuditEvent ->
                loggerAudit.getAuditString(sshAuditEvent, TransformableAuditLogType.SshAuditEvent));
  }

  public static String logFilenameBase() {
    return "sshd_log";
  }

  public String getUser() {
    return user;
  }

  public String getSession() {
    return session;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getCommand() {
    return command;
  }

  public String getWaitTime() {
    return waitTime;
  }

  public String getExecTime() {
    return execTime;
  }

  public String getResult() {
    return result;
  }
}
