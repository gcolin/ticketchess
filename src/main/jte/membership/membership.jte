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
!{if (seasons == null) seasons = java.util.List.of();}
!{if (memberships == null) memberships = java.util.List.of();}
!{String seasonParam = seasonId != null ? "?seasonId=" + seasonId : "";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "admin";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3 mt-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("breadcrumb.memberships")}</li>
      </ol>
    </nav>

    <form method="get" action="${contextPath}/membership" class="row g-2 align-items-end mb-3">
      @template.platform.seasonFilter(msg=msg, seasons=seasons, seasonId=seasonId, selectId="membership_season", autoSubmit=true)
    </form>

    <div class="mb-3 d-flex gap-2">
      <a class="btn btn-success" href="${contextPath}/membership/new">${msg.get("action.addMembership")}</a>
      <a class="btn btn-outline-success" href="${contextPath}/membership/export/csv${seasonParam}">${msg.get("action.exportCsv")}</a>
      <a class="btn btn-outline-success" href="${contextPath}/membership/export/pdf${seasonParam}">${msg.get("action.exportMembershipsPdf")}</a>
    </div>

    <div class="table-responsive bg-body rounded p-2">
      <table class="table table-striped align-middle mb-0">
        <thead>
          <tr>
            <th scope="col">${msg.get("label.action")}</th>
            <th scope="col">#</th>
            <th scope="col">${msg.get("admin.membership.user")}</th>
            <th scope="col">${msg.get("admin.membership.nrFfe")}</th>
            <th scope="col">${msg.get("label.lastname")}</th>
            <th scope="col">${msg.get("label.firstname")}</th>
            <th scope="col">${msg.get("player.birthdate")}</th>
            <th scope="col">${msg.get("label.status")}</th>
            <th scope="col">${msg.get("admin.membership.amount")}</th>
            <th scope="col">${msg.get("clubRegister.myMemberships.options")}</th>
          </tr>
        </thead>
        <tbody>
          @for(var mem : memberships)
            <tr>
              <td>
                <a class="btn btn-sm btn-outline-primary" href="${contextPath}/membership/${mem.getId()}/edit">${msg.get("action.edit")}</a>
              </td>
              <td>${mem.getId()}</td>
              <td>${mem.getUser() != null ? mem.getUser() : ""}</td>
              <td>${mem.getNrFfe() != null ? mem.getNrFfe() : ""}</td>
              <td>${mem.getLastname() != null ? mem.getLastname() : ""}</td>
              <td>${mem.getFirstname() != null ? mem.getFirstname() : ""}</td>
              <td>${mem.getBirthDate() != null ? mem.getBirthDate() : ""}</td>
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
                  <span class="text-muted">-</span>
                @endif
              </td>
            </tr>
          @else
            <tr>
              <td colspan="10" class="text-center text-muted">${msg.get("info.noMembership")}</td>
            </tr>
          @endfor
        </tbody>
      </table>
    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>