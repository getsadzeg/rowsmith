package rowsmith.internal.plan;

import java.util.function.IntFunction;

public record ValuePattern(String table, String column, IntFunction<?> byRowNumber) {}
