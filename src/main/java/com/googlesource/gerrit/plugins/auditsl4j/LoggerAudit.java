// Copyright (C) 2012 The Android Open Source Project
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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.gerrit.audit.AuditEvent;
import com.google.gerrit.audit.AuditListener;
import com.google.gerrit.extensions.annotations.Listen;
import com.google.inject.Singleton;

@Listen
@Singleton
public class LoggerAudit implements AuditListener {
  private static final Logger log = LoggerFactory.getLogger(LoggerAudit.class);
  private final SimpleDateFormat dateFmt = new SimpleDateFormat(
      "yyyy/MM/dd hh:mm:ss.SSSS");
  @SuppressWarnings("serial")
  private static final Map<Class<?>, AuditFormatter<?>> AUDIT_FORMATTERS =
      Collections
          .unmodifiableMap(new HashMap<Class<?>, AuditFormatter<? extends Object>>() {
            {
              put(HttpAuditEventFormat.CLASS, new HttpAuditEventFormat());
              put(RpcAuditEventFormat.CLASS, new RpcAuditEventFormat());
              put(SshAuditEventFormat.CLASS, new SshAuditEventFormat());
              put(AuditEventFormat.CLASS, new AuditEventFormat());
            }
          });

  static {
    log.info("EventId | EventTS | SessionId | User | Protocol data | Action | Parameters | Result | StartTS | Elapsed");
  }

  @Override
  public void onAuditableAction(AuditEvent action) {
    log.info(getFormattedAudit(action));
  }

  private String getFormattedAudit(AuditEvent action) {
    return String.format(
        "%1$s | %2$s | %3$s | %4$s | %5$s | %6$s | %7$s | %8$s | %9$s | %10$s",
        action.uuid.uuid(), getFormattedTS(action.when), action.sessionId,
        getFormattedAuditSingle(action.who), getFormattedAuditSingle(action),
        action.what, getFormattedAuditList(action.params),
        getFormattedAuditSingle(action.result),
        getFormattedTS(action.timeAtStart), action.elapsed);
  }

  private Object getFormattedAuditList(Multimap<String, ?> params) {
    if (params == null || params.size() == 0) {
      return "[]";
    }

    StringBuilder formattedOut = new StringBuilder("[");

    Set<String> paramNames = new TreeSet<>(params.keySet());

    int numParams = 0;
    for (String paramName : paramNames) {
      if (numParams++ > 0) {
        formattedOut.append(",");
      }
      formattedOut.append(paramName);
      formattedOut.append("=");
      formattedOut.append(getFormattedAudit(params.get(paramName)));
    }

    formattedOut.append(']');

    return formattedOut.toString();
  }

  private Object getFormattedAudit(Collection<? extends Object> values) {
    StringBuilder out = new StringBuilder();
    int numValues = 0;
    for (Object object : values) {
      if (numValues > 0) {
        out.append(",");
      }
      out.append(getFormattedAuditSingle(object));
      numValues++;
    }

    if (numValues > 1) {
      return "[" + out.toString() + "]";
    } else {
      return out.toString();
    }
  }

  private <T> String getFormattedAuditSingle(T result) {
    if (result == null) return "";

    @SuppressWarnings("unchecked")
    AuditFormatter<T> fmt =
        (AuditFormatter<T>) AUDIT_FORMATTERS.get(result.getClass());
    if (fmt == null) return result.toString();

    return fmt.format(result);
  }

  private synchronized String getFormattedTS(long when) {
    return dateFmt.format(new Date(when));
  }
}
