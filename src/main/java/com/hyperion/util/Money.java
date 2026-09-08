package com.hyperion.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
