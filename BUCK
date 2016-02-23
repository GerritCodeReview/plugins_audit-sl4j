include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//lib/maven.defs')

gerrit_plugin(
  name = 'audit-sl4j',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: audit-sl4j',
  ],
)

# this is required for bucklets/tools/eclipse/project.py to work
java_library(
  name = 'classpath',
  deps = [':audit-sl4j__plugin'],
)

