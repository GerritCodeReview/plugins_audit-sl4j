package com.googlesource.gerrit.plugins.auditsl4j;


import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

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
            dateFrom = format.parse( from );
        } catch (Exception e) {
            stdout.print("Invalid 'from' format: " + from + ", expected format <YYYY-MM-DD>");
            return;
        }
        Date dateUntil;
        try {
            dateUntil = format.parse( until );
        } catch (Exception e) {
            stdout.print("Invalid 'until' format: " + until + ", expected format <YYYY-MM-DD>");
            return;
        }

        if (dateFrom.after(dateUntil)) {
            stdout.print("'from' cannot be after 'until'");
            return;
        }

        Date currentDate = dateFrom;
        while ( currentDate.compareTo(dateUntil) <= 0 ) {
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
        transformLogs(currentDateString, "sshd_log", mapSshToAuditLogs);
    }

    private void transformHttpdLogs(String currentDateString) {
        transformLogs(currentDateString, "httpd_log", mapHttpToAuditLogs);
    }

    private void transformLogs(String currentDateString, String fileType, Function<String, String> logsMapping) {
        // Log format example: httpd_log.2019-01-19.gz
        String logFileName = sitePaths.logs_dir + "/" + fileType + "." + currentDateString + ".gz";
        //TODO audit log name should come from the plugin configuration
        String auditLogFileName = sitePaths.logs_dir + "/audit_log." + currentDateString + ".log";

        stdout.print("Transforming: " + logFileName + " => " + auditLogFileName + " ...\n");

        try{
            //TODO make sure we are appending
            GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(logFileName));
            BufferedReader input = new BufferedReader(new InputStreamReader(gzis));

            PrintWriter pw = new PrintWriter(Files.newBufferedWriter(
                    Paths.get(auditLogFileName)));

            input.lines().map(logsMapping).filter(Objects::nonNull).forEach(pw::println);
            // Make sure we flush the writer before going out of scope
            pw.flush();
        } catch (FileNotFoundException fnfe) {
            stdout.print("Cannot find '" + logFileName + "'. Skipping!\n");
        } catch (IOException e) {
            stdout.print("Error: " + e.getMessage() + "!\n");
        }
    }

    private Function<String, String> mapHttpToAuditLogs = (line) -> {
        AuditHTTPLog auditHTTPLog = AuditHTTPLog.parseHTTPLogs(line);
        try {
            return auditHTTPLog.toAuditLog(loggerAudit);
        } catch (Exception e) {
            e.printStackTrace(stdout);
            stdout.print("Error serializing HTTP log: " + e.getMessage() + "!\n");
        }
        return null;
    };

    private Function<String, String> mapSshToAuditLogs = (line) -> {
        AuditSshLog auditSshLog = AuditSshLog.parseSshLog(line);
        try {
            return auditSshLog.toAuditLog(loggerAudit);
        } catch (Exception e) {
            e.printStackTrace(stdout);
            stdout.print("Error serializing SSH log: " + e.getMessage() + "!\n");
        }
        return null;
    };

}