@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<com.github.gcolin.registration.PlayerSubscription> cancelledSubs
@param com.github.gcolin.event.Event event
@param java.util.List<com.github.gcolin.event.Event> events
@param java.util.List<com.github.gcolin.registration.PlayerSubscription> notPaidSubs
@param Object p
@param com.github.gcolin.player.DisplayPlayer player
@param com.github.gcolin.registration.PlayerSubscription sub
@param java.util.List<com.github.gcolin.registration.PlayerSubscription> subsWithoutPayment
!{i18ntitle = msg.get("page.dashboard");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "dashboard";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("page.dashboard")}</li>
      </ol>
    </nav>

    <div class="row g-3">

      <!-- Souscriptions sans paiement associé -->
      <div class="col-12">
        <div class="card rounded-bottom-0">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>${msg.get("dashboard.subsNoPayment")}</strong>
            <span class="badge bg-secondary">${subsWithoutPayment.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>${msg.get("player.name")}</th>
                    <th>${msg.get("player.firstname")}</th>
                    <th>${msg.get("sub.nrFFE")}</th>
                    <th>${msg.get("sub.creationUser")}</th>
                    <th>${msg.get("breadcrumb.events")}</th>
                    <th>${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var ps : subsWithoutPayment)
                    <tr>
                      <td>${ps.getId()}</td>
                      <td>${ps.getPlayer() != null && ps.getPlayer().getName() != null ? ps.getPlayer().getName() : ""}</td>
                      <td>${ps.getPlayer() != null && ps.getPlayer().getFirstname() != null ? ps.getPlayer().getFirstname() : ""}</td>
                      <td>${ps.getNrFfe() != null ? ps.getNrFfe() : ""}</td>
                      <td>${ps.getCreationUser() != null ? ps.getCreationUser() : ""}</td>
                      <td><a href="${contextPath}/event/${ps.getEvent().getId()}">@if(ps.getEvent().getName() != null)${ps.getEvent().getName()}@else${ps.getEvent().getId()}@endif</a></td>
                      <td><a class="btn btn-outline-primary btn-sm" href="${contextPath}/event/${ps.getEvent().getId()}/register/${ps.getId()}">${msg.get("action.edit")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="7" class="text-center text-muted">${msg.get("info.noSubscription")}</td></tr>
                  @endfor
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Souscriptions annulées -->
      <div class="col-12">
        <div class="card rounded-bottom-0">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>${msg.get("dashboard.cancelledSubs")}</strong>
            <span class="badge bg-danger">${cancelledSubs.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>${msg.get("player.name")}</th>
                    <th>${msg.get("player.firstname")}</th>
                    <th>${msg.get("sub.nrFFE")}</th>
                    <th>${msg.get("sub.creationUser")}</th>
                    <th>${msg.get("breadcrumb.events")}</th>
                    <th>${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var ps : cancelledSubs)
                    <tr>
                      <td>${ps.getId()}</td>
                      <td>${ps.getPlayer() != null && ps.getPlayer().getName() != null ? ps.getPlayer().getName() : ""}</td>
                      <td>${ps.getPlayer() != null && ps.getPlayer().getFirstname() != null ? ps.getPlayer().getFirstname() : ""}</td>
                      <td>${ps.getNrFfe() != null ? ps.getNrFfe() : ""}</td>
                      <td>${ps.getCreationUser() != null ? ps.getCreationUser() : ""}</td>
                      <td><a href="${contextPath}/event/${ps.getEvent().getId()}">@if(ps.getEvent().getName() != null)${ps.getEvent().getName()}@else${ps.getEvent().getId()}@endif</a></td>
                      <td><a class="btn btn-outline-primary btn-sm" href="${contextPath}/event/${ps.getEvent().getId()}/register/${ps.getId()}">${msg.get("action.edit")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="7" class="text-center text-muted">${msg.get("info.noSubscription")}</td></tr>
                  @endfor
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Souscriptions non payées -->
      <div class="col-12 mb-3">
        <div class="card rounded-bottom-0">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>${msg.get("dashboard.unpaidSubs")}</strong>
            <span class="badge bg-warning text-dark">${notPaidSubs.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>${msg.get("player.name")}</th>
                    <th>${msg.get("player.firstname")}</th>
                    <th>${msg.get("sub.nrFFE")}</th>
                    <th>${msg.get("sub.creationUser")}</th>
                    <th>${msg.get("breadcrumb.events")}</th>
                    <th>${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var ps : notPaidSubs)
                    <tr>
                      <td>${ps.getId()}</td>
                      <td>${ps.getPlayer() != null && ps.getPlayer().getName() != null ? ps.getPlayer().getName() : ""}</td>
                      <td>${ps.getPlayer() != null && ps.getPlayer().getFirstname() != null ? ps.getPlayer().getFirstname() : ""}</td>
                      <td>${ps.getNrFfe() != null ? ps.getNrFfe() : ""}</td>
                      <td>${ps.getCreationUser() != null ? ps.getCreationUser() : ""}</td>
                      <td><a href="${contextPath}/event/${ps.getEvent().getId()}">@if(ps.getEvent().getName() != null)${ps.getEvent().getName()}@else${ps.getEvent().getId()}@endif</a></td>
                      <td><a class="btn btn-outline-primary btn-sm" href="${contextPath}/event/${ps.getEvent().getId()}/register/${ps.getId()}">${msg.get("action.edit")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="7" class="text-center text-muted">${msg.get("info.noSubscription")}</td></tr>
                  @endfor
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>