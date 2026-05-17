package io.github.mundanej.mxjb.runtime;

/** Pull-reader event kinds understood by generated XML readers. */
public enum XmlEventKind {
  START_DOCUMENT,
  START_ELEMENT,
  TEXT,
  END_ELEMENT,
  END_DOCUMENT
}
