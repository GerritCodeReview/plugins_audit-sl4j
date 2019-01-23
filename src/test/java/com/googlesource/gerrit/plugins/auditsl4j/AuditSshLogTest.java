package com.googlesource.gerrit.plugins.auditsl4j;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuditSshLogTest {

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

        // [2019-01-23 12:44:04,723 +0100] b015fbe2 admin a/1000000 audit-sl4j.import.--from.rewew.--until.sdfds 2ms 2ms 0
        String logLine = String.format("[%s] %s %s %s %s %s %s %s", timestamp, session, user, accountId, command, waitTime, execTime, status);

        AuditSshLog expected = new AuditSshLog(timestamp,session,user,accountId, command,waitTime,execTime,status);
        AuditSshLog got = AuditSshLog.parseSshLog(logLine);

        assertEquals(expected.timestamp, got.timestamp);
        assertEquals(expected.session, got.session);
        assertEquals(expected.user, got.user);
        assertEquals(expected.accountId, got.accountId);
        assertEquals(expected.command, got.command);
        assertEquals(expected.waitTime, got.waitTime);
        assertEquals(expected.execTime, got.execTime);
        assertEquals(expected.status, got.status);
    }

    @Test
    public void handleLogout() {
        String logLine = "[2019-01-23 12:44:26,665 +0100] 70e3031f admin a/1000000 LOGOUT";
        AuditSshLog auditSshLog = AuditSshLog.parseSshLog(logLine);

        assertEquals("LOGOUT", auditSshLog.command);
    }
}
