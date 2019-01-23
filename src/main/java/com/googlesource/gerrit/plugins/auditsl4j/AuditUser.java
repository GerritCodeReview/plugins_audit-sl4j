package com.googlesource.gerrit.plugins.auditsl4j;

import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.GroupMembership;

public class AuditUser extends CurrentUser {
  String username;

  @Override
  public GroupMembership getEffectiveGroups() {
    return null;
  }

  @Override
  public String getUserName() {
    return username;
  }
}
