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

import static com.googlesource.gerrit.plugins.auditsl4j.AuditFormatters.getFormattedAuditSingle;
import static com.googlesource.gerrit.plugins.auditsl4j.AuditFormatters.getFormattedTS;

import com.google.common.collect.Multimap;
import com.google.gerrit.audit.AuditEvent;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class AuditRendererToCsv implements AuditRenderer {

  @Override
  public String render(AuditEvent auditEvent) {
    return String.format(
        "%1$s | %2$s | %3$s | %4$s | %5$s | %6$s | %7$s | %8$s | %9$s | %10$s",
        auditEvent.uuid.uuid(),
        getFormattedTS(auditEvent.when),
        auditEvent.sessionId,
        getFormattedAuditSingle(auditEvent.who),
        getFormattedAuditSingle(auditEvent),
        auditEvent.what,
        getFormattedAuditList(auditEvent.params),
        getFormattedAuditSingle(auditEvent.result),
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
      out.append(getFormattedAuditSingle(object));
      numValues++;
    }

    if (numValues > 1) {
      return "[" + out.toString() + "]";
    }
    return out.toString();
  }
}
