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
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.dashboard");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.dashboard"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.dashboard"), helpText=msg.get("admin.tile.dashboardDesc"), icon="bi-speedometer2")

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <form method="get" action="${contextPath}/dashboard" class="d-flex flex-wrap align-items-center gap-2 mb-0">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="dashboard_season", autoSubmit=false, inline=true)
          <button type="submit" class="btn  btn-secondary">${msg.get("action.applyFilters")}</button>
        </form>
      `)
    `)

    @template.platform.adminPanel(
      heading=msg.get("dashboard.subsNoPayment"),
      subheading=msg.get("dashboard.subsNoPaymentCount", subsWithoutPayment.size()),
      content = @`
        <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">${msg.get("player.name")}</th>
                    <th scope="col">${msg.get("player.firstname")}</th>
                    <th scope="col">${msg.get("sub.nrFFE")}</th>
                    <th scope="col">${msg.get("sub.creationUser")}</th>
                    <th scope="col">${msg.get("breadcrumb.events")}</th>
                    <th scope="col">${msg.get("label.action")}</th>
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
                      <td><a class="btn btn-sm btn-outline-primary" href="${contextPath}/event/${ps.getEvent().getId()}/register/${ps.getId()}">${msg.get("action.edit")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="7" class="text-center text-muted">${msg.get("info.noSubscription")}</td></tr>
                  @endfor
                </tbody>
              </table>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("dashboard.cancelledSubs"),
      subheading=msg.get("dashboard.cancelledSubsCount", cancelledSubs.size()),
      content = @`
        <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">${msg.get("player.name")}</th>
                    <th scope="col">${msg.get("player.firstname")}</th>
                    <th scope="col">${msg.get("sub.nrFFE")}</th>
                    <th scope="col">${msg.get("sub.creationUser")}</th>
                    <th scope="col">${msg.get("breadcrumb.events")}</th>
                    <th scope="col">${msg.get("label.action")}</th>
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
                      <td><a class="btn btn-sm btn-outline-primary" href="${contextPath}/event/${ps.getEvent().getId()}/register/${ps.getId()}">${msg.get("action.edit")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="7" class="text-center text-muted">${msg.get("info.noSubscription")}</td></tr>
                  @endfor
                </tbody>
              </table>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("dashboard.unpaidSubs"),
      subheading=msg.get("dashboard.unpaidSubsCount", notPaidSubs.size()),
      content = @`
        <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">${msg.get("player.name")}</th>
                    <th scope="col">${msg.get("player.firstname")}</th>
                    <th scope="col">${msg.get("sub.nrFFE")}</th>
                    <th scope="col">${msg.get("sub.creationUser")}</th>
                    <th scope="col">${msg.get("breadcrumb.events")}</th>
                    <th scope="col">${msg.get("label.action")}</th>
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
                      <td><a class="btn btn-sm btn-outline-primary" href="${contextPath}/event/${ps.getEvent().getId()}/register/${ps.getId()}">${msg.get("action.edit")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="7" class="text-center text-muted">${msg.get("info.noSubscription")}</td></tr>
                  @endfor
                </tbody>
              </table>
      `
    )
  `
)
