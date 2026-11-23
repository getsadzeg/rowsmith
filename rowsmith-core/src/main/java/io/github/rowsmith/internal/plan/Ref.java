package io.github.rowsmith.internal.plan;

import io.github.rowsmith.Row;

sealed interface Ref {

    record Planned(Node node) implements Ref {}

    record Existing(Row row) implements Ref {}
}
