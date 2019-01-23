package com.googlesource.gerrit.plugins.auditsl4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;

public class AuditSshLogTest {

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

    AuditSshLog expected =
        new AuditSshLog(timestamp, session, user, accountId, command, waitTime, execTime, status);

    Optional<AuditSshLog> maybeAuditSshLog = AuditSshLog.createFromLog(logLine);
    assertTrue("Didn't create AuditSshLog", maybeAuditSshLog.isPresent());
    AuditSshLog gotAuditSshLog = maybeAuditSshLog.get();
    assertEquals(expected.timestamp, gotAuditSshLog.timestamp);
    assertEquals(expected.session, gotAuditSshLog.session);
    assertEquals(expected.user, gotAuditSshLog.user);
    assertEquals(expected.accountId, gotAuditSshLog.accountId);
    assertEquals(expected.command, gotAuditSshLog.command);
    assertEquals(expected.waitTime, gotAuditSshLog.waitTime);
    assertEquals(expected.execTime, gotAuditSshLog.execTime);
    assertEquals(expected.status, gotAuditSshLog.status);
  }

  @Test
  public void handleLogout() {
    String logLine = "[2019-01-23 12:44:26,665 +0100] 70e3031f admin a/1000000 LOGOUT";
    Optional<AuditSshLog> maybeAuditSshLog = AuditSshLog.createFromLog(logLine);
    assertTrue("Didn't create AuditSshLog", maybeAuditSshLog.isPresent());
    assertEquals("LOGOUT", maybeAuditSshLog.get().command);
  }

  @Test(expected = Test.None.class /* no exception expected */)
  public void correctlyTransform_SshAuditEvent() throws Exception {
    AuditSshLog auditSshLog =
        new AuditSshLog(
            "2019-01-23 12:44:26,665 +0100",
            "70e3031f",
            "admin",
            "a/1000000",
            "LOGOUT",
            "2ms",
            "2ms",
            "0");

    String auditLog = auditSshLog.toAuditLog(loggerAudit);

    assertTrue("'type' not matched: " + auditLog, auditLog.contains("\"type\":\"SshAuditEvent\""));
    assertTrue(
        "'access_path' not matched: " + auditLog,
        auditLog.contains("\"access_path\":\"SSH_COMMAND\""));
    assertTrue("'when' not matched: " + auditLog, auditLog.contains("\"when\":1548243866665"));
  }
}
