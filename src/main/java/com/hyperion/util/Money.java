package com.hyperion.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Converts monetary values between the domain representation (BigDecimal) and
 * the SQLite representation (integer cents). No monetary value is read or
 * persisted as a floating-point number.
 */
public final class Money {

    private static final int SCALE = 2;

    private Money() {
    }

    public static void setCents(PreparedStatement statement, int parameterIndex, BigDecimal value) throws SQLException {
        statement.setLong(parameterIndex, toCents(value));
    }

    public static long toCents(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("O valor monetário não pode ser nulo.");
        }

        return value.movePointRight(SCALE)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, SCALE);
    }

    public static BigDecimal getCents(ResultSet resultSet, String columnLabel) throws SQLException {
        return fromCents(resultSet.getLong(columnLabel));
    }

    public static boolean hasAtMostTwoFractionDigits(BigDecimal value) {
        if (value == null) {
            return false;
        }

        try {
            value.setScale(SCALE, RoundingMode.UNNECESSARY);
            return true;
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    public static boolean fitsInCents(BigDecimal value) {
        if (value == null || !hasAtMostTwoFractionDigits(value)) {
            return false;
        }
        try {
            toCents(value);
            return true;
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    /**
     * Allocates a monetary total across weighted values without losing cents.
     * The largest fractional remainders receive the remaining cents, with a
     * deterministic tie-breaker based on the original list order.
     */
    public static List<BigDecimal> allocateProportionally(BigDecimal total, List<BigDecimal> weights) {
        if (total == null || weights == null || total.signum() < 0 || !hasAtMostTwoFractionDigits(total)) {
            throw new IllegalArgumentException("Total inválido para rateio monetário.");
        }

        long totalCents = toCents(total);
        List<Long> weightCents = new ArrayList<>(weights.size());
        long weightSum = 0;
        for (BigDecimal weight : weights) {
            if (weight == null || weight.signum() < 0 || !hasAtMostTwoFractionDigits(weight)) {
                throw new IllegalArgumentException("Peso inválido para rateio monetário.");
            }
            long cents = toCents(weight);
            weightCents.add(cents);
            weightSum = Math.addExact(weightSum, cents);
        }

        if (totalCents == 0 && weightCents.isEmpty()) {
            return List.of();
        }
        if (weightSum == 0 || totalCents > weightSum) {
            throw new IllegalArgumentException("O total deve estar entre zero e a soma dos pesos.");
        }

        BigInteger divisor = BigInteger.valueOf(weightSum);
        BigInteger dividendTotal = BigInteger.valueOf(totalCents);
        List<Allocation> allocations = new ArrayList<>(weightCents.size());
        long allocatedCents = 0;

        for (int index = 0; index < weightCents.size(); index++) {
            BigInteger dividend = BigInteger.valueOf(weightCents.get(index)).multiply(dividendTotal);
            BigInteger[] division = dividend.divideAndRemainder(divisor);
            long cents = division[0].longValueExact();
            allocations.add(new Allocation(index, cents, division[1]));
            allocatedCents = Math.addExact(allocatedCents, cents);
        }

        long remainingCents = totalCents - allocatedCents;
        allocations.sort(Comparator.comparing(Allocation::remainder).reversed().thenComparing(Allocation::index));
        for (int index = 0; index < remainingCents; index++) {
            Allocation allocation = allocations.get(index);
            allocations.set(index, allocation.withCents(Math.addExact(allocation.cents(), 1)));
        }

        BigDecimal[] result = new BigDecimal[allocations.size()];
        for (Allocation allocation : allocations) {
            result[allocation.index()] = fromCents(allocation.cents());
        }
        return List.of(result);
    }

    private record Allocation(int index, long cents, BigInteger remainder) {
        private Allocation withCents(long value) {
            return new Allocation(index, value, remainder);
        }
    }
}
