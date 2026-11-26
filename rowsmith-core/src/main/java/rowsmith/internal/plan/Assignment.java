package rowsmith.internal.plan;

import rowsmith.internal.schema.ForeignKey;

sealed interface Assignment {

    record Value(Object value) implements Assignment {}

    record Null() implements Assignment {}

    record FromParent(ForeignKey fk, int index) implements Assignment {}

    record Preallocated(Object value) implements Assignment {}

    record Default() implements Assignment {}
}
