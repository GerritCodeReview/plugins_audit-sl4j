// Copyright (C) 2018 The Android Open Source Project
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

import com.google.gerrit.server.util.SystemLog;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

@Singleton
public class AuditWriterToAsyncAppender implements AuditWriter {
  private final Logger log = Logger.getLogger(LoggerAudit.AUDIT_LOGGER_NAME);
  private final AsyncAppender appender;

  @Inject
  public AuditWriterToAsyncAppender(AuditConfig config, SystemLog systemLog) {
    String logName = config.getLogName().get();
    appender = systemLog.createAsyncAppender(logName, new PatternLayout());
  }

  @Override
  public void write(String auditBody) {
    appender.append(newLoggingEvent(auditBody));
  }

  private LoggingEvent newLoggingEvent(String auditBody) {
    return new LoggingEvent( //
        LoggerAudit.AUDIT_LOGGER_NAME,
        log, // logger
        TimeUtil.nowMs(), // when
        Level.INFO, // level
        auditBody, // message text
        "HTTPD", // thread name
        null, // exception information
        null, // current NDC string
        null, // caller location
        null // MDC properties
        );
  }
}
