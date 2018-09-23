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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class AuditFormatters {
  private static final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSSS");

  @SuppressWarnings("serial")
  private static final Map<Class<?>, AuditFormatter<?>> AUDIT_FORMATTERS =
      Collections.unmodifiableMap(
          new HashMap<Class<?>, AuditFormatter<? extends Object>>() {
            {
              put(HttpAuditEventFormat.CLASS, new HttpAuditEventFormat());
              put(RpcAuditEventFormat.CLASS, new RpcAuditEventFormat());
              put(SshAuditEventFormat.CLASS, new SshAuditEventFormat());
              put(AuditEventFormat.CLASS, new AuditEventFormat());
            }
          });

  public static <T> String getFormattedAuditSingle(T result) {
    if (result == null) return "";

    @SuppressWarnings("unchecked")
    AuditFormatter<T> fmt = (AuditFormatter<T>) AUDIT_FORMATTERS.get(result.getClass());
    if (fmt == null) return result.toString();

    return fmt.format(result);
  }

  public static synchronized String getFormattedTS(long when) {
    return dateFmt.format(new Date(when));
  }
}
