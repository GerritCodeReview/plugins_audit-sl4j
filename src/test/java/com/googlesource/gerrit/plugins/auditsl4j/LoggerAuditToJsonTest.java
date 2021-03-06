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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.Sandboxed;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.common.Version;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.audit.AuditListener;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import org.junit.Test;

@Sandboxed
@TestPlugin(
    name = "audit-sl4j",
    sysModule = "com.googlesource.gerrit.plugins.auditsl4j.LoggerAuditToJsonTest$TestModule")
public class LoggerAuditToJsonTest extends LightweightPluginDaemonTest implements WaitForCondition {

  @Inject @CanonicalWebUrl private String webUrl;

  public static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(AuditWriter.class).to(AuditWriterToStringList.class);
      bind(AuditFormatRenderer.class).to(AuditRendererToJson.class);
      DynamicSet.bind(binder(), AuditListener.class).to(LoggerAudit.class);
    }
  }

  @Test
  public void testHttpJsonAudit() throws Exception {
    AuditWriterToStringList auditStrings = getPluginInstance(AuditWriterToStringList.class);

    anonymousRestSession.get("/config/server/version").assertOK();
    waitForFirstAuditRecord(auditStrings);

    String auditJsonString = auditStrings.strings.get(0);
    assertValidJson(auditJsonString);
    assertThat(auditJsonString).contains(Version.getVersion());
  }

  @Test
  public void testHttpCsvAuditShouldContainAnonymousUser() throws Exception {
    AuditWriterToStringList auditStrings = getPluginInstance(AuditWriterToStringList.class);

    anonymousRestSession.get("/config/server/version").assertOK();
    waitForFirstAuditRecord(auditStrings);

    assertThat(auditStrings.strings.get(0).toLowerCase()).contains("anonymous");
  }

  @Test
  public void testHttpCsvAuditShouldContainIdentifiedUser() throws Exception {
    AuditWriterToStringList auditStrings = getPluginInstance(AuditWriterToStringList.class);

    userRestSession.get("/config/server/version").assertOK();

    waitForFirstAuditRecord(auditStrings);

    assertThat(auditStrings.strings.get(0).toLowerCase()).contains(user.id().toString());
  }

  private <T> T getPluginInstance(Class<T> clazz) {
    return plugin.getSysInjector().getInstance(clazz);
  }

  private void waitForFirstAuditRecord(AuditWriterToStringList auditStrings) {
    assertThat(waitFor(() -> auditStrings.strings.size() >= 1)).isTrue();
  }

  private JsonObject assertValidJson(String auditJsonString) {
    JsonObject auditJson = new Gson().fromJson(auditJsonString, JsonObject.class);
    assertThat(auditJson).isNotNull();
    return auditJson;
  }
}
