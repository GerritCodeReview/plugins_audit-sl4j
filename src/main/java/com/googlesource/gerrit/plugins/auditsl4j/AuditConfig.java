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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import java.util.Optional;

public class AuditConfig {
  private final PluginConfig config;

  @Inject
  public AuditConfig(@PluginName String pluginName, PluginConfigFactory configFactory) {
    config = configFactory.getFromGerritConfig(pluginName);
  }

  public AuditFormatTypes getFormat() {
    return config.getEnum("format", AuditFormatTypes.CSV);
  }

  public Optional<String> getLogName() {
    return Optional.ofNullable(config.getString("logName"));
  }
}
