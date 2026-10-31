# Mise and Elide Build Migration

Use mise locally and in GitHub Actions, pinning Elide 1.6.1 and its Python component. Use Elide directly for JVM compilation, tests, coverage, JARs, and native images. Avoid a separate Java toolchain: an isolated build has already succeeded with JAVA_HOME unset and only system C tools on PATH.

Implementation:
1. Add mise.toml and a multi-platform mise.lock, shared tasks, and a CI setup action.
2. Configure Elide's JUnit XML and XML/HTML coverage reports. Preserve the 85% line / 70% branch gate with a tested XML checker until runtime#2691 is implemented.
3. Run integration scripts with Elide Python, Java compilation/execution with Elide, and embedded Native Image. Forward only required tool environment variables until runtime#2690 is implemented. Python test discovery is already tracked by runtime#1475.
4. Move the container build to Elide and remove full-JDK setup from CI; keep LLVM 23 and Rust interoperability checks.
5. Test with no external JDK, validate workflows, review, run full CI, and integrate once green and warning-free.

Keep Maven as an optional compatibility build for now; it is no longer the primary local or CI path. Do not weaken tests, coverage gates, or the native interoperability matrix. File reproducible Elide bugs and needed enhancements in elide-dev/runtime; do not patch the runtime repository in this task.
