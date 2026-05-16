# Coding-agent handoff

This file gives the next exact sequence of tasks. Agents must not skip ahead to product implementation.

## Current repository state

- Design-Control Pack v0.1 scaffold exists.
- Product implementation is intentionally absent.
- Gradle 9.5.1 module structure, quality tooling, dependency verification, dependency locking, offline helper scripts, CI skeleton, ADRs, and documentation scaffolds exist.

## Task sequence

1. `TASK-0001`: Validate Design-Control Pack v0.1 file presence and consistency. Completed for the scaffold.
2. `TASK-0002`: Hydrate and verify the Gradle wrapper and dependency metadata. Completed for the scaffold; repeat when dependencies change.
3. `TASK-0003`: Run and harden Gradle quality-gate wiring without product code. Completed for the scaffold.
4. `TASK-0004`: Convert staged build policies into failing gates where meaningful. Partially complete; coverage thresholds remain intentionally staged while modules are empty.
5. `TASK-0005`: Perform phase-one readiness review and open implementation task cards. Still pending.

No task in this sequence implements XML schema parsing, binding, reading, writing, or validation.

## Implementation unlock criteria

Implementation may begin only after:

- Design-Control Pack v0.1 is accepted.
- Build scaffold sanity tasks pass.
- ADRs are approved.
- Phase-one requirement IDs are accepted.
- Module boundary tests are staged.
- A phase-one implementation task card with allowed files is approved.
