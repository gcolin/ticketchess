package com.github.gcolin.membership;

import com.github.gcolin.membership.License;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.platform.AbstractDao;

public class LicenseDao extends AbstractDao<License> {

    public LicenseDao() {
        super(License.class);
    }
}
