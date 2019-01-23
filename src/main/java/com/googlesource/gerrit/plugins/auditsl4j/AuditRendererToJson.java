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

import com.google.common.collect.ListMultimap;
import com.google.gerrit.audit.AuditEvent;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.AccessPath;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.OutputFormat;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

public class AuditRendererToJson implements AuditFormatRenderer {
  private final ExclusionStrategy INCLUDE_ONLY_WHITELISTED =
      new ExclusionStrategy() {
        private final HashSet<Class<?>> WHITELIST_CLASSES =
            new HashSet<>(
                Arrays.asList(
                    String.class,
                    Object.class,
                    CurrentUser.class,
                    Long.class,
                    Long.TYPE,
                    Integer.class,
                    Integer.TYPE,
                    AccessPath.class,
                    CurrentUser.PropertyKey.class,
                    Account.Id.class,
                    AuditRecord.class));
        private final HashSet<String> BLACKLIST_FIELDS =
            new HashSet<>(Arrays.asList("anonymousCowardName"));

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
          return BLACKLIST_FIELDS.contains(f.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
          return !AuditEvent.class.isAssignableFrom(clazz)
              && !CurrentUser.class.isAssignableFrom(clazz)
              && !ListMultimap.class.isAssignableFrom(clazz)
              && !AuditEvent.UUID.class.isAssignableFrom(clazz)
              && !WHITELIST_CLASSES.contains(clazz);
        }
      };

  private final Gson gson =
      OutputFormat.JSON_COMPACT
          .newGsonBuilder()
          .setExclusionStrategies(INCLUDE_ONLY_WHITELISTED)
          .create();

  @Override
  public String render(AuditEvent auditEvent) {
    return gson.toJson(new AuditRecord(auditEvent));
  }

  @Override
  public String render(AuditEvent auditEvent, String type) {
    return gson.toJson(new AuditRecord(auditEvent, type));
  }

    @Override
  public Optional<String> headers() {
    return Optional.empty();
  }
}
