package io.github.rowsmith.internal.plan;

import io.github.rowsmith.internal.schema.ColumnDef;

sealed interface Cell {

    record Known(Object value) implements Cell {}

    record Pending(Node node, ColumnDef column) implements Cell {
        @Override
        public String toString() {
            return "<" + node.label + "." + column.name + ">";
        }
    }

    record Unknown() implements Cell {}

    default boolean isNull() {
        return this instanceof Known k && k.value() == null;
    }
}
