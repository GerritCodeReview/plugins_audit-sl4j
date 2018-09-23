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
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.common.Version;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

@TestPlugin(
    name = "audit-sl4j",
    sysModule = "com.googlesource.gerrit.plugins.auditsl4j.LoggerAuditTest$TestModule")
public class LoggerAuditTest extends LightweightPluginDaemonTest {

  @Inject @CanonicalWebUrl private String webUrl;

  public static class TestModule extends Module {

    @Override
    protected void configure() {
      bind(AuditWriter.class).to(AuditWriterToStringList.class);
      super.configure();
    }
  }

  @Singleton
  public static class AuditWriterToStringList implements AuditWriter {
    public final List<String> strings = new ArrayList<>();

    @Override
    public void write(String msg) {
      strings.add(msg);
    }
  }

  @Test
  public void testHttpAudit() throws Exception {
    AuditWriterToStringList auditStrings = getPluginInstance(AuditWriterToStringList.class);

    Request.Get(webUrl + "config/server/version").execute().returnResponse();

    assertThat(auditStrings.strings).hasSize(2);
    assertThat(auditStrings.strings.get(1)).contains(Version.getVersion());
  }

  private <T> T getPluginInstance(Class<T> clazz) {
    return plugin.getSysInjector().getInstance(clazz);
  }
}
