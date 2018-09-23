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

import com.google.gerrit.audit.AuditEvent;
import com.google.gerrit.server.OutputFormat;
import com.google.gson.Gson;
import java.util.Optional;

public class AuditRendererToJson implements AuditRenderer {
  private final Gson gson = OutputFormat.JSON_COMPACT.newGsonBuilder().create();

  @Override
  public String render(AuditEvent auditEvent) {
    return gson.toJson(auditEvent);
  }

  @Override
  public Optional<String> headers() {
    return Optional.empty();
  }
}
