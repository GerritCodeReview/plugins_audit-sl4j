# Audit plugin for Gerrit

Plug into the Gerrit Code Review audit extension and format and forward all
the events to an [SLF4J appender](https://www.slf4j.org), the same logging
system used internally for the generation of all logs..

## How to configure logging?

Audit events are by default stored into the Gerrit error_log under the appender
`com.googlesource.gerrit.plugins.auditsl4j.LoggerAudit`. However, it is possible
to generate a separate audit file and having the audit records formatted into
CSV or JSON.

For more details, see the [configuration guide](src/main/resources/Documentation/config.md)
in the plugin.

## Why auditing events on Gerrit?

Audits leave an immutable trace of what happened on Gerrit and allows to answer
the question "who did what and when?".

When aggregated by time, project or user, can give an overall figure on how much
the system is utilized and allow to make a better planning of the hardware resources allocation
and planning the downtimes to reduce the impact on the people and projects.

## Is this a duplicate of Gerrit stream events?

Gerrit stream events are triggered on Git-related operations and reviews, but do
not cover most of the actions that happen on Gerrit and do not include all the system
information that is typically needed for audit-trail purposes.

Furthermore, stream events are designed to be consumed in near-real-time while audits,
are typically archived and consumed off-line.

## What the audit events look like?

The format of Gerrit audits changes across the different releases because they reflect
the internal representation of the Java objects in memory.

They share an overall basic structure:

- type: Audit java class
- event: Audit event
  - session_id: unique identifier of the user's session
  - who: user that generated the event
    - account_id: user unique id
    - access_path: how the user accessed the system
  - when: epoch time-stamp of the event
  - what: action performed
  - result: result of the action
  - time_at_start: epoch time-stamp of when the action started
  - elapsed: how long the action lasted (msec)
  - uuid: audit event UUID

Example audit of a login from a Git client over SSH:
```json
{
  "type": "SshAuditEvent",
  "event": {
    "session_id": "0261c43e",
    "who": {
      "account_id": {
        "id": 1011575
      },
      "access_path": "GIT",
      "last_login_external_id_property_key": {}
    },
    "when": 1539561891898,
    "what": "LOGOUT",
    "params": {},
    "result": "0",
    "time_at_start": 1539561891898,
    "elapsed": 0,
    "uuid": {
      "uuid": "audit:f135cb10-59be-4087-a9e0-571680b93a59"
    }
  }
}
```

Example audit of a Gerrit changes query over SSH:
```json
{
  "type": "SshAuditEvent",
  "event": {
    "session_id": "22688824",
    "who": {
      "account_id": {
        "id": 1011203
      },
      "access_path": "SSH_COMMAND",
      "last_login_external_id_property_key": {}
    },
    "when": 1539561891503,
    "what": "gerrit.query.--format.json.--current-patch-set.project:mycompany/myproject commit:798b22fcf3614e8575e0ef23019a9706b8acebcc NOT is:draft",
    "params": {},
    "result": "0",
    "time_at_start": 1539561891503,
    "elapsed": 3,
    "uuid": {
      "uuid": "audit:171a9b6f-327a-40a2-9b66-c47a39eba68c"
    }
  }
}
```
