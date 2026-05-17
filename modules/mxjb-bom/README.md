# mxjb-bom

Dependency alignment BOM for published `mundane XSD Java Binding` artifacts.

## Current status

The BOM constrains the published `mundane XSD Java Binding` modules and excludes
internal conformance/example projects. It should track public artifacts as they
become publication candidates; it is not a place for test fixtures, examples, or
internal harness modules.

## Contributor notes

- Add new published artifacts to the BOM in the same change that introduces them.
- Do not add internal test harnesses or top-level examples to the BOM.
