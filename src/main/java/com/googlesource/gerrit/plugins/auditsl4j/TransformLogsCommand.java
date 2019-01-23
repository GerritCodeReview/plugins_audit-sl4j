package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@CommandMetaData(name = "import", description = "Transform ssh and http logs into audit logs")
public class TransformLogsCommand extends SshCommand {

    private final SitePaths sitePaths;

    @Inject
    public TransformLogsCommand(SitePaths sitePaths) {
        this.sitePaths = sitePaths;
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

        stdout.print(from + " - " + until + "!\n");
    }

    //TODO Also parse SSH logs!!
    private void tranformHttpdLogs(Date from, Date until) {
        loadLogs(format.format(from),format.format(until));
    }
    private void loadLogs(String from, String until) {
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
            SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z");
            Date dateWhen = format.parse( auditHTTPLog.timestamp );
            AuditLogEvent ale = new AuditLogEvent(auditHTTPLog.method, auditHTTPLog.status, dateWhen.getTime());
            return new AuditLog("ExtendedHttpAuditEvent", ale).serialize();
        } catch (Exception e) {
            e.printStackTrace(stdout);
            stdout.print("Error serializing HTTP log: " + e.getMessage() + "!\n");
        }
        return null;
    };

}