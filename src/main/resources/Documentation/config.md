Audit Configuration
===================

File `gerrit.config`
--------------------

The audit configuration can be defined in the main gerrit.config
in a specific section dedicated to the audit-sl4j plugin.

plugin.@PLUGIN@.format
:	Output format of the audit record. Can be set to either JSON
    or CSV. By default, CSV.
    
plugin.@PLUGIN@.logName
:	Write audit to a separate log name under Gerrit logs directory.
    By default, audit records are put into the error_log.
