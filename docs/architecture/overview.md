# Architecture overview

The system is a compiler pipeline plus a small explicit runtime.

```text
XSD files / catalogs / binding config
        |
        v
Schema resolver and source loader
        |
        v
Schema parser frontend
        |
        v
XSD component graph
        |
        v
Normalized schema IR
        |
        v
Binding model
        |
        v
Java model / codec / validation model
        |
        v
Generated source files
        |
        v
Compile-time verification + runtime tests + native-image tests
```

## Main architectural commitments

- Generated code uses explicit methods, not reflection-based binding.
- Generated codecs target project-owned XML event/output interfaces.
- Runtime core has no third-party dependencies.
- Generator may use dependencies, but must not leak them into generated runtime paths.
- Validation is modeled as a generated validation plan from day one.
- The schema component graph and binding model are separate from any concrete XML parser library.

`docs/architecture/complexity-review.md` records the post-1.0.0 simplicity review and the preferred
order for future refactors that make the current full-profile implementation easier to test without
changing product behavior.
