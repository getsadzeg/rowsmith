package rowsmith.internal.plan;

import rowsmith.Row;

sealed interface Ref {

    record Planned(Node node) implements Ref {}

    record Existing(Row row) implements Ref {}
}
