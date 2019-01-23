package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gerrit.audit.SshAuditEvent;
import com.google.gerrit.server.AccessPath;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public AuditSshLog(
      String timestamp,
      String session,
      String user,
      String accountId,
      String command,
      String waitTime,
      String execTime,
      String status) {
    this.user = user;
    this.session = session;
    this.timestamp = timestamp;
    this.accountId = accountId;
    this.command = command;
    this.waitTime = waitTime;
    this.execTime = execTime;
    this.status = status;
  }

  public static Optional<AuditSshLog> createFromLog(String line) {
    Matcher a =
        Pattern.compile(
                "^\\[(?<timestamp>.*?)\\]\\s(?<session>.*?)\\s"
                    + "(?<user>.*?)\\s(?<accountId>.*?)\\s(?<command>.*?)(?:\\s"
                    + "(?<waitTime>.*?)\\s(?<execTime>.*?)\\s(?<status>.*?))?$")
            .matcher(line);

    if (a.matches()) {
      try {
        return Optional.of(
            new AuditSshLog(
                a.group("timestamp"),
                a.group("session"),
                a.group("user"),
                a.group("accountId"),
                a.group("command"),
                a.group("waitTime"),
                a.group("execTime"),
                a.group("status")));
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
    au.username = this.user;
    au.setAccessPath(AccessPath.SSH_COMMAND);
    return au;
  }

  private long getWhen() throws ParseException {
    // 2019-01-23 12:44:04,723 +0100
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS Z");
    return format.parse(this.timestamp).getTime();
  }

  public String toAuditLog(LoggerAudit loggerAudit) throws ParseException {
    // XXX 'elapse' is not correct since it is a final value calculated this way: TimeUtil.nowMs() -
    // when;
    SshAuditEvent sshAuditEvent =
        new SshAuditEvent(this.session, getAuditUser(), this.command, getWhen(), null, null);
    return loggerAudit.getAuditString(sshAuditEvent, TransformableAuditLogType.SshAuditEvent);
  }

  public String fileType() {
    return "sshd_log";
  }
}
