package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gerrit.audit.SshAuditEvent;
import com.google.gerrit.server.AccessPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuditSshLog implements TransformableLog {
    private static final Logger log = LoggerFactory.getLogger(AuditSshLog.class);

    String user;
    String session;
    String timestamp;
    String accountId;
    String command;
    String waitTime;
    String execTime;
    String status;

    public AuditSshLog(String timestamp, String session, String user, String accountId, String command, String waitTime, String execTime, String status) {
        this.user = user;
        this.session = session;
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.command = command;
        this.waitTime = waitTime;
        this.execTime = execTime;
        this.status = status;
    }

    static public AuditSshLog parseSshLog(String line) {
        Matcher a =
            Pattern.compile("^\\[(.*?)\\]\\s(.*?)\\s(.*?)\\s(.*?)\\s(.*?)(?:\\s(.*?)\\s(.*?)\\s(.*?))?$")
                .matcher(line);

        if (a.matches()) {
            try {
                return new AuditSshLog(a.group(1), a.group(2), a.group(3), a.group(4), a.group(5), a.group(6), a.group(7), a.group(8));
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
        au.setAccessPath(AccessPath.SSH_COMMAND);
        return au;
    }

    public String toAuditLog(LoggerAudit loggerAudit) {
        SshAuditEvent sshAuditEvent = new SshAuditEvent(this.session, getAuditUser(), this.command, 1L, null, null);
        return loggerAudit.getAuditString(sshAuditEvent, TransformableAuditLogType.SshAuditEvent);
    }
}
