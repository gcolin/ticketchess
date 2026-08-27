package com.github.gcolin.membership;

import com.github.gcolin.membership.MembershipOption;
import com.github.gcolin.platform.Transactional;
import com.github.gcolin.platform.AbstractDao;

public class MembershipOptionDao extends AbstractDao<MembershipOption> {

    public MembershipOptionDao() {
        super(MembershipOption.class);
    }
}
