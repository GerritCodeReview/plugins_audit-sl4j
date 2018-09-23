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
        "Gerrit-Module: com.googlesource.gerrit.plugins.auditsl4j.LoggerAudit$Module",
        "Implementation-Title: Gerrit Audit provider for SLF4J",
        "Implementation-URL: https://gerrit.googlesource.com/plugins/audit-sl4j/",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [ ],
)

junit_tests(
    name = "audit_sl4j_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    visibility = ["//visibility:public"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":audit-sl4j__plugin",
    ],
)
