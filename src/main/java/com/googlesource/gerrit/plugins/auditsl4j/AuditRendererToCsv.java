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

import com.google.common.collect.Multimap;
import com.google.gerrit.audit.AuditEvent;
import com.google.gerrit.audit.ExtendedHttpAuditEvent;
import com.google.gerrit.audit.HttpAuditEvent;
import com.google.gerrit.audit.RpcAuditEvent;
import com.google.gerrit.audit.SshAuditEvent;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class AuditRendererToCsv implements AuditRenderer {
  
  private static final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSSS");

  @SuppressWarnings("serial")
  private static final Map<Class<?>, CsvFieldFormatter<?>> FIELD_CSV_FORMATTERS =
      Collections.unmodifiableMap(
          new HashMap<Class<?>, CsvFieldFormatter<? extends Object>>() {
            {
              put(HttpAuditEvent.class, new HttpAuditEventFormat());
              put(ExtendedHttpAuditEvent.class, new HttpAuditEventFormat());
              put(RpcAuditEvent.class, new RpcAuditEventFormat());
              put(SshAuditEvent.class, new SshAuditEventFormat());
              put(AuditEvent.class, new AuditEventFormat());
            }
          });
  
  interface CsvFieldFormatter<T> {
    String formatToCsv(T result);
  }
  
  static class RpcAuditEventFormat implements CsvFieldFormatter<RpcAuditEvent> {
    @Override
    public String formatToCsv(RpcAuditEvent result) {
      return "RPC-" + result.httpMethod + ", Status:" + result.httpStatus;
    }
  }
  
  static class HttpAuditEventFormat implements CsvFieldFormatter<HttpAuditEvent> {

    @Override
    public String formatToCsv(HttpAuditEvent result) {
      return "HTTP-" + result.httpMethod + ", Status:" + result.httpStatus;
    }
  }
  
  static class SshAuditEventFormat implements CsvFieldFormatter<SshAuditEvent> {
    @Override
    public String formatToCsv(SshAuditEvent result) {
      return "SSH";
    }
  }
  
  static class AuditEventFormat implements CsvFieldFormatter<SshAuditEvent> {

    @Override
    public String formatToCsv(SshAuditEvent result) {
      return "";
    }
  }

  @Override
  public String render(AuditEvent auditEvent) {
    return String.format(
        "%1$s | %2$s | %3$s | %4$s | %5$s | %6$s | %7$s | %8$s | %9$s | %10$s",
        auditEvent.uuid.uuid(),
        getFormattedTS(auditEvent.when),
        auditEvent.sessionId,
        getFieldAsCsv(auditEvent.who),
        getFieldAsCsv(auditEvent),
        auditEvent.what,
        getFormattedAuditList(auditEvent.params),
        getFieldAsCsv(auditEvent.result),
        getFormattedTS(auditEvent.timeAtStart),
        auditEvent.elapsed);
  }

  @Override
  public Optional<String> headers() {
    return Optional.of(
        "EventId | EventTS | SessionId | User | Protocol data | Action | Parameters | Result | StartTS | Elapsed");
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
      out.append(getFieldAsCsv(object));
      numValues++;
    }

    if (numValues > 1) {
      return "[" + out.toString() + "]";
    }
    return out.toString();
  }
  

  public static <T> String getFieldAsCsv(T result) {
    if (result == null) return "";

    @SuppressWarnings("unchecked")
    CsvFieldFormatter<T> fmt = (CsvFieldFormatter<T>) FIELD_CSV_FORMATTERS.get(result.getClass());
    if (fmt == null) return result.toString();

    return fmt.formatToCsv(result);
  }

  public static synchronized String getFormattedTS(long when) {
    return dateFmt.format(new Date(when));
  }
}
