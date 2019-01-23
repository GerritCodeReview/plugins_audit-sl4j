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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.googlesource.gerrit.plugins.auditsl4j.AuditRendererToJson;
import com.googlesource.gerrit.plugins.auditsl4j.AuditWriterToStringList;
import com.googlesource.gerrit.plugins.auditsl4j.LoggerAudit;
import java.util.Optional;
import org.junit.Test;

public class SSHLogTest {

  LoggerAudit loggerAudit =
      new LoggerAudit(new AuditWriterToStringList(), new AuditRendererToJson());

  @Test
  public void successfullyParseSshLog() {
    String user = "myUser";
    String timestamp = "2019-01-23 12:43:53,115 +0100";
    String session = "b015fbe2";
    String accountId = "a/1000000";
    String command = "audit-sl4j.import.--from.rewew.--until.sdfds";
    String waitTime = "2ms";
    String execTime = "2ms";
    String status = "0";

    // [2019-01-23 12:44:04,723 +0100] b015fbe2 admin a/1000000
    // audit-sl4j.import.--from.rewew.--until.sdfds 2ms 2ms 0
    String logLine =
        String.format(
            "[%s] %s %s %s %s %s %s %s",
            timestamp, session, user, accountId, command, waitTime, execTime, status);

    SSHLog expected =
        new SSHLog(timestamp, session, user, accountId, command, waitTime, execTime, status);

    Optional<SSHLog> maybeSshLog = SSHLog.createFromLog(logLine);
    assertTrue("Didn't create SSHLog", maybeSshLog.isPresent());
    SSHLog gotSSHLog = maybeSshLog.get();
    assertEquals(expected.getTimestamp(), gotSSHLog.getTimestamp());
    assertEquals(expected.getSession(), gotSSHLog.getSession());
    assertEquals(expected.getUser(), gotSSHLog.getUser());
    assertEquals(expected.getAccountId(), gotSSHLog.getAccountId());
    assertEquals(expected.getCommand(), gotSSHLog.getCommand());
    assertEquals(expected.getWaitTime(), gotSSHLog.getWaitTime());
    assertEquals(expected.getExecTime(), gotSSHLog.getExecTime());
    assertEquals(expected.getStatus(), gotSSHLog.getStatus());
  }

  @Test
  public void handleLogout() {
    String logLine = "[2019-01-23 12:44:26,665 +0100] 70e3031f admin a/1000000 LOGOUT";
    Optional<SSHLog> maybeSshLog = SSHLog.createFromLog(logLine);
    assertTrue("Didn't create SSHLog", maybeSshLog.isPresent());
    assertEquals("LOGOUT", maybeSshLog.get().getCommand());
  }

  @Test(expected = Test.None.class /* no exception expected */)
  public void correctlyTransform_SshAuditEvent() throws Exception {
    SSHLog SSHLog =
        new SSHLog(
            "2019-01-23 12:44:26,665 +0100",
            "70e3031f",
            "admin",
            "a/1000000",
            "LOGOUT",
            "2ms",
            "2ms",
            "0");

    Optional<String> maybeAuditLog = SSHLog.toAuditLog(loggerAudit);
    assertTrue("Didn't Audit Log", maybeAuditLog.isPresent());

    String auditLog = maybeAuditLog.get();
    assertTrue("'type' not matched: " + auditLog, auditLog.contains("\"type\":\"SshAuditEvent\""));
    assertTrue(
        "'access_path' not matched: " + auditLog,
        auditLog.contains("\"access_path\":\"SSH_COMMAND\""));
    assertTrue("'when' not matched: " + auditLog, auditLog.contains("\"when\":1548243866665"));
  }
}
