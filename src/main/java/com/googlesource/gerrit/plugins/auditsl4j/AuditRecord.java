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

import com.google.gerrit.server.AuditEvent;

public class AuditRecord {
  public final String type;
  public final AuditEvent event;

  public AuditRecord(AuditEvent event) {
    super();

    String eventClass = event.getClass().getName();
    this.type = eventClass.substring(eventClass.lastIndexOf('.') + 1);
    this.event = event;
  }

  public AuditRecord(AuditEvent event, TransformableAuditLogType type) {
    super();

    this.type = type.name();
    this.event = event;
  }
}
