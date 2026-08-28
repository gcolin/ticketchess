@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.Membership membership
@param com.github.gcolin.membership.MembershipStatus[] statuses
!{i18ntitle = msg.get("page.membershipNew");}
!{title = i18ntitle;}
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
        <li class="breadcrumb-item"><a href="${contextPath}/membership">${msg.get("breadcrumb.memberships")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("page.membershipNew")}</li>
      </ol>
    </nav>

    <div class="bg-body rounded p-3">
      <form method="post" action="${contextPath}/membership">
        <div class="row g-3">
          <div class="col-12 col-md-6">
            <label for="membership_user" class="form-label">${msg.get("admin.membership.user")}</label>
            <input id="membership_user" type="text" name="user" class="form-control" required>
          </div>

          <div class="col-12 col-md-6">
            <label for="membership_nrffe" class="form-label">${msg.get("admin.membership.nrFfe")}</label>
            <input id="membership_nrffe" type="text" name="nrFfe" class="form-control" required>
          </div>

          <div class="col-12 col-md-6">
            <label for="membership_lastname" class="form-label">${msg.get("label.lastname")}</label>
            <input id="membership_lastname" type="text" name="lastname" class="form-control">
          </div>

          <div class="col-12 col-md-6">
            <label for="membership_firstname" class="form-label">${msg.get("label.firstname")}</label>
            <input id="membership_firstname" type="text" name="firstname" class="form-control">
          </div>

          <div class="col-12 col-md-6">
            <label for="membership_birthdate" class="form-label">${msg.get("player.birthdate")}</label>
            <input id="membership_birthdate" type="date" name="birthDate" class="form-control">
          </div>

          <div class="col-12 col-md-6">
            <label for="membership_amount" class="form-label">${msg.get("admin.membership.amountCents")}</label>
            <input id="membership_amount" type="number" name="amountCents" class="form-control" min="0" required>
          </div>

          <div class="col-12 col-md-6">
            <label for="membership_status" class="form-label">${msg.get("label.status")}</label>
            <select id="membership_status" name="status" class="form-select" required>
              @for(var memStatus : statuses)
                <option value="${memStatus.name()}">${msg.get("clubRegister.membershipStatus." + memStatus)}</option>
              @endfor
            </select>
          </div>
        </div>

        <div class="d-flex justify-content-end gap-2 mt-4">
          <a class="btn btn-outline-secondary" href="${contextPath}/membership">${msg.get("action.cancel")}</a>
          <button type="submit" class="btn btn-primary">${msg.get("action.createGeneric")}</button>
        </div>
      </form>
    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>