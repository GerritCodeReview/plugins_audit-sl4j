package com.googlesource.gerrit.plugins.auditsl4j;

import java.text.ParseException;

public interface TransformableLog {
  String toAuditLog(LoggerAudit loggerAudit) throws ParseException;
}
