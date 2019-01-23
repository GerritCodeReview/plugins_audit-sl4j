package com.googlesource.gerrit.plugins.auditsl4j;


import com.google.gerrit.audit.AuditEvent;
import com.google.gerrit.server.AccessPath;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import static com.google.gerrit.audit.AuditEvent.UNKNOWN_SESSION_ID;

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

        tranformHttpdLogs(dateFrom, dateUntil);
        tranformSshdLogs(dateFrom, dateUntil);

        stdout.print(from + " - " + until + "!\n");
    }

    private void tranformSshdLogs(Date fromDate, Date untilDate) { }

    //TODO Also parse SSH logs!!
    private void tranformHttpdLogs(Date fromDate, Date untilDate) {
        String from = format.format(fromDate);
        String until = format.format(untilDate);
        // httpd_log.2019-01-19.gz
        String httpLogFileName = sitePaths.logs_dir + "/httpd_log." + from + ".gz";
        //TODO audit log name should come from the plugin configuration
        String auditLogFileName = sitePaths.logs_dir + "/audit_log." + from + ".log";

        try{
            GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(httpLogFileName));
            BufferedReader input = new BufferedReader(new InputStreamReader(gzis));

            PrintWriter pw = new PrintWriter(Files.newBufferedWriter(
                            Paths.get(auditLogFileName)));

            input.lines().map(mapToAuditLog).filter(Objects::nonNull).forEach(pw::println);
            // Make sure we flush the writer before going out of scope
            pw.flush();
        } catch (IOException e) {
            stdout.print("Error: " + e.getMessage() + "!\n");
        }
    }
    private Function<String, String> mapToAuditLog = (line) -> {
        AuditHTTPLog auditHTTPLog = AuditHTTPLog.parseHTTPLogs(line);
        try {
            return auditHTTPLog.toAuditLog(loggerAudit);
        } catch (Exception e) {
            e.printStackTrace(stdout);
            stdout.print("Error serializing HTTP log: " + e.getMessage() + "!\n");
        }
        return null;
    };

}