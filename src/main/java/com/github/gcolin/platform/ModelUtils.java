package com.github.gcolin.platform;

import com.github.gcolin.membership.LicensePriceCalculator;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ModelUtils {

    private static Set<String> youngCategories = new HashSet<String>();

    static {
        youngCategories.addAll(Arrays.asList(
                "PpoM", "PpoF", "PouM", "PouF", "PupM", "PupF", "MinM", "MinF", "BenM", "BenF", "CadM", "CadF", "JunM",
                "JunF"));
    }

    public static boolean isYoung(String category) {
        return youngCategories.contains(category);
    }

    public static String getCategory(LocalDate currentDate, int birthyear, boolean gender) {
        int years = currentDate.getYear() - birthyear;
        if (currentDate.getMonthValue() < 9) {
            years--;
        }
        if (years < 8) {
            return gender ? "PpoM" : "PpoF";
        } else if (years < 10) {
            return gender ? "PouM" : "PouF";
        } else if (years < 12) {
            return gender ? "PupM" : "PupF";
        } else if (years < 14) {
            return gender ? "BenM" : "BenF";
        } else if (years < 16) {
            return gender ? "MinM" : "MinF";
        } else if (years < 18) {
            return gender ? "CadM" : "CadF";
        } else if (years < 20) {
            return gender ? "JunM" : "JunF";
        } else if (years < 50) {
            return gender ? "SenM" : "SenF";
        } else if (years < 65) {
            return gender ? "SepM" : "SepF";
        } else {
            return gender ? "VetM" : "VetF";
        }
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * @deprecated Use {@link LicensePriceCalculator} or {@link com.github.gcolin.membership.LicensePriceService} instead.
     * License prices should be retrieved from the database.
     */
    @Deprecated(since = "1.0.1", forRemoval = true)
    public static int getLicensePrice(String category, char licenseType) {
        return LicensePriceCalculator.getPrice(category, licenseType);
    }
}
