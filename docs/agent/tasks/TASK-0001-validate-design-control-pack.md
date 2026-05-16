# TASK-0001: validate-design-control-pack


Requirement IDs: `REQ-BUILD-001`, `REQ-DOC-001`, `REQ-AGENT-001`
ADR IDs: `ADR-0011`, `ADR-0013`
Allowed files: documentation index files, `DESIGN_CONTROL_PACK_v0.1.md`, `README.md`
Forbidden files: `modules/**/src/main/java/**`, `modules/**/src/test/java/**`
Expected behavior: confirm all required pack documents exist and cross-references are coherent.
Tests to add/update: none unless adding a documentation validation script.
Commands to run: `./gradlew designControlStatus`, `./gradlew projects`
Acceptance criteria: checklist in `DESIGN_CONTROL_PACK_v0.1.md` can be reviewed without missing files.
Rollback notes: revert documentation-only changes.

