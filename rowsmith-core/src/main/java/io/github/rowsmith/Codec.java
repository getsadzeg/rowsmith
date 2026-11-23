package io.github.rowsmith;

import java.util.ArrayList;
import java.util.List;

sealed interface Codec<T> {

    Object toDb(T value);

    T fromDb(Object raw);

    record Plain<T>(Class<T> type) implements Codec<T> {
        @Override
        public Object toDb(T value) {
            return value;
        }

        @Override
        public T fromDb(Object raw) {
            return type.cast(raw);
        }
    }

    record Labels<E extends Enum<E> & PgEnum>(Class<E> type) implements Codec<E> {
        @Override
        public Object toDb(E value) {
            return value == null ? null : value.label();
        }

        @Override
        public E fromDb(Object raw) {
            if (raw == null) return null;
            for (E e : type.getEnumConstants()) if (e.label().equals(raw)) return e;
            throw new RowsmithException(ErrorCode.NOT_IN_ENUM, "'" + raw + "' is not a label of " + type.getName()
                    + "; regenerate the code after a schema change");
        }
    }

    record ListOf<E>(Codec<E> element) implements Codec<List<E>> {
        @Override
        public Object toDb(List<E> value) {
            if (value == null) return null;
            List<Object> out = new ArrayList<>(value.size());
            for (E e : value) out.add(element.toDb(e));
            return out;
        }

        @Override
        public List<E> fromDb(Object raw) {
            if (raw == null) return null;
            List<E> out = new ArrayList<>();
            for (Object o : (List<?>) raw) out.add(element.fromDb(o));
            return out;
        }
    }
}
