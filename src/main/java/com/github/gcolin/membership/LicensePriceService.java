package com.github.gcolin.membership;

import com.github.gcolin.platform.RequestContext;
import java.util.List;

public class LicensePriceService {

    /**
     * Récupère le prix d'une licence pour une catégorie donnée.
     *
     * @param category La catégorie (ex: PpoM, SenF, etc.)
     * @param licenseType Le type de licence ('A' ou 'B')
     * @return Le prix en centimes, ou null si non trouvé
     */
    public Integer getLicensePrice(String category, char licenseType) {
        List<License> licenses = RequestContext.require().licenseDao().all();
        License license = licenses.stream()
                .filter(l -> String.valueOf(licenseType).equals(l.getName()))
                .findFirst()
                .orElse(null);

        if (license == null) {
            return null;
        }

        List<LicensePrice> prices = RequestContext.require().licensePriceDao().all();
        return prices.stream()
                .filter(p -> p.getCategory().equals(category) && p.getLicense().getId().equals(license.getId()))
                .map(LicensePrice::getPriceCents)
                .findFirst()
                .orElse(null);
    }

    /**
     * Récupère le prix d'une licence pour une catégorie donnée.
     *
     * @param category La catégorie (ex: PpoM, SenF, etc.)
     * @param licenseName Le nom/type de licence ('A' ou 'B')
     * @return Le prix en centimes, ou null si non trouvé
     */
    public Integer getLicensePriceByName(String category, String licenseName) {
        return getLicensePrice(category, licenseName.charAt(0));
    }
}
