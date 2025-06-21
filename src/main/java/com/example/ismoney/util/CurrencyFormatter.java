package com.example.ismoney.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class untuk formatting currency dalam format Rupiah
 */
public class CurrencyFormatter {

    private static final DecimalFormat formatter;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        formatter = new DecimalFormat("#,##0", symbols);
    }

    /**
     * Format angka menjadi format Rupiah
     * @param amount jumlah uang
     * @return String dengan format "Rp 1.000.000"
     */
    public static String formatToRupiah(double amount) {
        return "Rp " + formatter.format(amount);
    }

    /**
     * Format angka menjadi format Rupiah tanpa prefix
     * @param amount jumlah uang
     * @return String dengan format "1.000.000"
     */
    public static String formatNumber(double amount) {
        return formatter.format(amount);
    }

    /**
     * Parse string currency menjadi double
     * @param currencyString string dalam format "Rp 1.000.000" atau "1.000.000"
     * @return double value
     */
    public static double parseFromRupiah(String currencyString) {
        if (currencyString == null || currencyString.trim().isEmpty()) {
            return 0.0;
        }

        // Remove "Rp" prefix and whitespace
        String cleanString = currencyString.replace("Rp", "").trim();

        // Replace dots with empty string and commas with dots for parsing
        cleanString = cleanString.replace(".", "").replace(",", ".");

        try {
            return Double.parseDouble(cleanString);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}