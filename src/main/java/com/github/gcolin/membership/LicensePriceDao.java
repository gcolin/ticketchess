package com.github.gcolin.membership;

import com.github.gcolin.membership.LicensePrice;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.platform.AbstractDao;

public class LicensePriceDao extends AbstractDao<LicensePrice> {

    public LicensePriceDao() {
        super(LicensePrice.class);
    }
}
