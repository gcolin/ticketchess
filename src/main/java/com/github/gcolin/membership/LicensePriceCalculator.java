package com.github.gcolin.membership;

/**
 * Calcul des prix des licences basé sur la catégorie et le type.
 * Cette classe contient la logique d'évaluation des tarifs.
 */
public class LicensePriceCalculator {

    public static int getPriceForLicenseA(String category) {
        String prefix = category.substring(0, 3);
        boolean isU8U16 = "Ppo".equals(prefix) || "Pou".equals(prefix) || "Pup".equals(prefix) ||
                          "Min".equals(prefix) || "Ben".equals(prefix);
        boolean isU18U20 = "Jun".equals(prefix) || "Cad".equals(prefix);

        if (isU8U16) {
            return 1800;
        } else if (isU18U20) {
            return 2800;
        } else {
            return 5200;
        }
    }

    public static int getPriceForLicenseB(String category) {
        String prefix = category.substring(0, 3);
        boolean isU8U16 = "Ppo".equals(prefix) || "Pou".equals(prefix) || "Pup".equals(prefix) ||
                          "Min".equals(prefix) || "Ben".equals(prefix);
        boolean isU18U20 = "Jun".equals(prefix) || "Cad".equals(prefix);

        if (isU8U16 || isU18U20) {
            return 300;
        } else {
            return 1000;
        }
    }

    public static int getPrice(String category, char licenseType) {
        if (licenseType == 'A') {
            return getPriceForLicenseA(category);
        } else {
            return getPriceForLicenseB(category);
        }
    }
}
