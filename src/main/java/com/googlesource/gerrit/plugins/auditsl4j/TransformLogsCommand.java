// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.auditsl4j.logsource.HTTPLog;
import com.googlesource.gerrit.plugins.auditsl4j.logsource.SSHLog;
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

@CommandMetaData(name = "transform", description = "Transform ssh and http logs into audit logs")
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
      stderr.print("Invalid 'from' format: " + from + ", expected format <YYYY-MM-DD>");
      return;
    }
    Date dateUntil;
    try {
      dateUntil = format.parse(until);
    } catch (Exception e) {
      stderr.print("Invalid 'until' format: " + until + ", expected format <YYYY-MM-DD>");
      return;
    }

    if (dateFrom.after(dateUntil)) {
      stderr.print("'from' cannot be after 'until'");
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
    transformLogs(currentDateString, SSHLog.logFilenameBase(), SSHLog::createFromLog);
  }

  private void transformHttpdLogs(String currentDateString) {
    transformLogs(currentDateString, HTTPLog.logFilenameBase(), HTTPLog::createFromLog);
  }

  private void transformLogs(
      String currentDateString,
      String fileType,
      Function<String, Optional<? extends TransformableLog>> createTransformable) {
    // Log format example: httpd_log.2019-01-19.gz
    String logFileName = sitePaths.logs_dir + "/" + fileType + "." + currentDateString + ".gz";
    String auditLogFileName = sitePaths.logs_dir + "/audit_log." + currentDateString + ".log";

    stdout.print("Transforming: " + logFileName + " => " + auditLogFileName + " ...\n");
    stdout.flush();

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
          .map(
              maybeTransformableLog ->
                  maybeTransformableLog.flatMap(
                      transformableLog -> transformableLog.toAuditLog(loggerAudit)))
          .forEach(pw::println);
      pw.flush();
    } catch (FileNotFoundException fnfe) {
      stderr.print("Cannot find '" + logFileName + "'. Skipping!\n");
    } catch (IOException e) {
      stderr.print("Error: " + e.getMessage() + "!\n");
    }
  }
}
