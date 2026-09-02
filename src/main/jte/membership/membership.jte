@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.Membership membership
@param java.util.Map<String, java.util.List<com.github.gcolin.membership.MembershipOptionSubscription>> membershipSubscriptions
@param java.util.List<com.github.gcolin.membership.Membership> memberships
@param Object p
@param String status
@param com.github.gcolin.registration.PlayerSubscription sub
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.memberships");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
!{java.util.List<com.github.gcolin.membership.Membership> safeMemberships = memberships != null ? memberships : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("breadcrumb.memberships"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.memberships"), helpText=msg.get("help.memberships"), icon="bi-person-vcard")

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <form method="get" action="${contextPath}/membership" class="d-flex align-items-center mb-0">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="membership_season", autoSubmit=true, inline=true)
        </form>
        <div class="d-flex align-items-center gap-2 ms-md-auto">
          <a class="btn btn-success" href="${contextPath}/membership/new${seasonId != null ? "?seasonId=" + seasonId : ""}">${msg.get("action.addMembership")}</a>
          @template.membership.membershipPageActionsMenu(contextPath=contextPath, msg=msg, seasonId=seasonId)
        </div>
      `)
      <table class="table table-striped align-middle mb-0 mt-3">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("admin.membership.user")}</th>
            <th scope="col">${msg.get("admin.membership.nrFfe")}</th>
            <th scope="col">${msg.get("label.lastname")}</th>
            <th scope="col">${msg.get("label.firstname")}</th>
            <th scope="col">${msg.get("player.birthdate")}</th>
            <th scope="col">${msg.get("admin.membership.licenseType")}</th>
            <th scope="col">${msg.get("label.season")}</th>
            <th scope="col">${msg.get("label.status")}</th>
            <th scope="col">${msg.get("admin.membership.amount")}</th>
            <th scope="col">${msg.get("clubRegister.myMemberships.options")}</th>
            <th scope="col">${msg.get("label.action")}</th>
          </tr>
        </thead>
        <tbody>
          @for(var mem : safeMemberships)
            <tr>
              <td>${mem.getId()}</td>
              <td>${mem.getUser() != null ? mem.getUser() : ""}</td>
              <td>${mem.getNrFfe() != null ? mem.getNrFfe() : ""}</td>
              <td>${mem.getLastname() != null ? mem.getLastname() : ""}</td>
              <td>${mem.getFirstname() != null ? mem.getFirstname() : ""}</td>
              <td>${mem.getBirthDate() != null ? mem.getBirthDate() : ""}</td>
              <td>
                @if(mem.getLicenseType() != null && !mem.getLicenseType().isBlank())
                  ${msg.get("clubRegister.membershipOptions.license" + mem.getLicenseType())}
                @else
                  <span class="text-body-secondary">—</span>
                @endif
              </td>
              <td>${mem.getSeason() != null && mem.getSeason().getName() != null ? mem.getSeason().getName() : ""}</td>
              <td>${mem.getStatus() != null ? msg.get("clubRegister.membershipStatus." + mem.getStatus()) : ""}</td>
              <td>${mem.getAmountCents() / 100.0} €</td>
              <td>
                @if(membershipSubscriptions != null && membershipSubscriptions.get(String.valueOf(mem.getId())) != null)
                  <ul class="mb-0 ps-3">
                    @for(var mos : membershipSubscriptions.get(String.valueOf(mem.getId())))
                      <li>${mos.getMembershipOption() != null && mos.getMembershipOption().getOptionValue() != null ? mos.getMembershipOption().getOptionValue() : ""}</li>
                    @endfor
                  </ul>
                @else
                  <span class="text-body-secondary">—</span>
                @endif
              </td>
              <td>
                @template.membership.membershipRowActionsMenu(contextPath=contextPath, msg=msg, membership=mem, seasonId=seasonId)
              </td>
            </tr>
          @else
            <tr>
              <td colspan="12" class="text-center text-body-secondary">${msg.get("info.noMembership")}</td>
            </tr>
          @endfor
        </tbody>
      </table>
    `)
  `
)
