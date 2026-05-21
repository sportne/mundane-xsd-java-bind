# Standards baseline

## Primary baseline

| Standard | Project role |
|---|---|
| XML 1.0 Fifth Edition | Primary XML syntax baseline. |
| Namespaces in XML 1.0 Third Edition | Primary namespace baseline. |
| XML Schema 1.0 Structures and Datatypes | Primary schema language baseline. |
| XML Infoset | Abstract XML information model used to avoid overfitting to lexical representation. |
| XML Base | Base URI and resource resolution considerations. |
| xml:id | Future ID semantics and validation considerations. |

## Secondary baseline

| Standard / API | Project role |
|---|---|
| XPath | Test-oracle language and XSD 1.0 identity-constraint selector/field subset reference. |
| SAX/StAX/DOM | Reference concepts and optional tooling/adapters, not generated-code architecture. |
| JAXB/Jakarta XML Binding | Ecosystem reference, not an API clone target. |

XML 1.1 and XML Schema 1.1 are not project targets. The full-standard program is limited to XML
1.0 plus XML Schema 1.0.

## Standards-control requirements

- Each supported feature must map to a compatibility profile.
- Each supported feature must have tests and conformance matrix entries.
- Unsupported features must be rejected with explicit diagnostics, not silently ignored.

## Local reference cache

Agents may keep local, non-authoritative copies of dated standards documents under `.repo/standards/`. The `.repo/` directory is ignored by Git, so these files are for offline reading only and must not be cited as project-controlled source material. The normative references remain the public standards named in this document and the accepted ADRs.
