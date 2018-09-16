load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

gerrit_plugin(
    name = "audit-sl4j",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: audit-sl4j",
        "Gerrit-ReloadMode: reload",
        "Implementation-Title: Gerrit Audit provider for SLF4J",
        "Implementation-URL: https://gerrit.googlesource.com/plugins/audit-sl4j/",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [ ],
)
