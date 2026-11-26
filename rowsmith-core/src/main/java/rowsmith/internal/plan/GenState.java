package rowsmith.internal.plan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rowsmith.internal.schema.UniqueKey;

final class GenState {
    final Map<String, Long> numbers = new HashMap<>();
    final Map<UniqueKey, Map<List<Object>, Ref>> unique = new HashMap<>();

    long nextNumber(String table) {
        return numbers.merge(table, 1L, Long::sum);
    }

    void numberUsed(String table, long number) {
        numbers.merge(table, number, Math::max);
    }

    GenState copy() {
        GenState s = new GenState();
        s.numbers.putAll(numbers);
        unique.forEach((k, v) -> s.unique.put(k, new HashMap<>(v)));
        return s;
    }

    void adopt(GenState other) {
        numbers.clear();
        numbers.putAll(other.numbers);
        unique.clear();
        unique.putAll(other.unique);
    }
}
