package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import org.kohsuke.args4j.Option;

@CommandMetaData(name = "import", description = "Transform ssh and http logs into audit logs")
public class TransformLogsCommand extends SshCommand {

  private LoggerAudit loggerAudit;
  private final SitePaths sitePaths;

  @Inject
  public TransformLogsCommand(SitePaths sitePaths, LoggerAudit loggerAudit) {
    this.sitePaths = sitePaths;
    this.loggerAudit = loggerAudit;
  }

  SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

  @Option(name = "--from", usage = "transform logs from <YYYY-MM-DD>")
  private String from;

  @Option(name = "--until", usage = "transform logs until <YYYY-MM-DD>")
  private String until;

  @Override
  public void run() {

    Date dateFrom;
    try {
      dateFrom = format.parse(from);
    } catch (Exception e) {
      stdout.print("Invalid 'from' format: " + from + ", expected format <YYYY-MM-DD>");
      return;
    }
    Date dateUntil;
    try {
      dateUntil = format.parse(until);
    } catch (Exception e) {
      stdout.print("Invalid 'until' format: " + until + ", expected format <YYYY-MM-DD>");
      return;
    }

    if (dateFrom.after(dateUntil)) {
      stdout.print("'from' cannot be after 'until'");
      return;
    }

    Date currentDate = dateFrom;
    while (currentDate.compareTo(dateUntil) <= 0) {
      transformHttpdLogs(format.format(currentDate));
      transformSshdLogs(format.format(currentDate));

      currentDate = getTomorrowDate(currentDate);
    }

    stdout.print("Transformed HTTP and SSH logs from " + from + " until " + until + "!\n");
  }

  private Date getTomorrowDate(Date currentDate) {
    Calendar c = Calendar.getInstance();
    c.setTime(currentDate);
    c.add(Calendar.DAY_OF_MONTH, 1);
    return c.getTime();
  }

  private void transformSshdLogs(String currentDateString) {
    transformLogs(currentDateString, "sshd_log", AuditSshLog::createFromLog);
  }

  private void transformHttpdLogs(String currentDateString) {
    transformLogs(currentDateString, "httpd_log", AuditHTTPLog::createFromLog);
  }

  private void transformLogs(
      String currentDateString,
      String fileType,
      Function<String, Optional<? extends TransformableLog>> createTransformable) {
    // Log format example: httpd_log.2019-01-19.gz
    String logFileName = sitePaths.logs_dir + "/" + fileType + "." + currentDateString + ".gz";
    // TODO audit log name should come from the plugin configuration
    String auditLogFileName = sitePaths.logs_dir + "/audit_log." + currentDateString + ".log";

    stdout.print("Transforming: " + logFileName + " => " + auditLogFileName + " ...\n");

    try {
      GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(logFileName));
      BufferedReader input = new BufferedReader(new InputStreamReader(gzis));

      PrintWriter pw =
          new PrintWriter(
              Files.newBufferedWriter(
                  Paths.get(auditLogFileName),
                  StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE,
                  StandardOpenOption.APPEND));

      input
          .lines()
          .map(createTransformable)
          .map(maybeTransformableLog -> maybeTransformableLog.flatMap(mapToAuditLogs))
          .forEach(pw::println);
      // Make sure we flush the writer before going out of scope
      pw.flush();
    } catch (FileNotFoundException fnfe) {
      stdout.print("Cannot find '" + logFileName + "'. Skipping!\n");
    } catch (IOException e) {
      stdout.print("Error: " + e.getMessage() + "!\n");
    }
  }

  private Function<TransformableLog, Optional<String>> mapToAuditLogs =
      (transformableLog) -> {
        try {
          return Optional.ofNullable(transformableLog.toAuditLog(loggerAudit));
        } catch (Exception e) {
          stdout.print("Error serializing HTTP log: " + e.getMessage() + "!\n");
          return Optional.empty();
        }
      };
}
