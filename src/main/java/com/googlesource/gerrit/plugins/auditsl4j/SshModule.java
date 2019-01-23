package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gerrit.sshd.PluginCommandModule;

public class SshModule extends PluginCommandModule {
  @Override
  protected void configureCommands() {
    command(TransformLogsCommand.class);
  }
}
