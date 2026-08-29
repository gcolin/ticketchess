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
@param double totalPayments
@param double unpaidPlayerSubscriptions
@param java.util.List<com.github.gcolin.event.Event> closestEvents
@param java.util.List<Object[]> topEvents
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
@param Object p
!{i18ntitle = msg.get("page.statistics");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
!{java.util.List<com.github.gcolin.event.Event> safeClosestEvents = closestEvents != null ? closestEvents : java.util.List.of();}
!{java.util.List<Object[]> safeTopEvents = topEvents != null ? topEvents : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.statistics"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.statistics"), helpText=msg.get("admin.tile.statisticsDesc"), icon="bi-bar-chart")

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <form method="get" action="${contextPath}/statistics" class="d-flex flex-wrap align-items-center gap-2 mb-0">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="statistics_season", autoSubmit=false, inline=true)
          <button type="submit" class="btn  btn-secondary">${msg.get("action.applyFilters")}</button>
        </form>
      `)
    `)

    @template.platform.adminPanel(
      heading=msg.get("statistics.totalPayments"),
      content = @`
        <h3 class="display-6 text-success mb-0">${String.format(java.util.Locale.US, "%.2f", totalPayments)} €</h3>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("statistics.totalPayments") + " + " + msg.get("statistics.unpaidSubscriptions"),
      content = @`
        <h3 class="display-6 text-info mb-0">${String.format(java.util.Locale.US, "%.2f", totalPayments + unpaidPlayerSubscriptions)} €</h3>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("statistics.closestEvents"),
      subheading=msg.get("statistics.closestEventsCount", safeClosestEvents.size()),
      content = @`
        <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">${msg.get("event.name")}</th>
                    <th scope="col">${msg.get("event.start")}</th>
                    <th scope="col">${msg.get("event.end")}</th>
                    <th scope="col">${msg.get("event.status")}</th>
                    <th scope="col">${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var ev : safeClosestEvents)
                    <tr>
                      <td>${ev.getId()}</td>
                      <td>${ev.getName() != null ? ev.getName() : ""}</td>
                      <td>
                        @if(ev.getStartDate() != null)${String.valueOf(ev.getStartDate())}@endif
                      </td>
                      <td>
                        @if(ev.getEndDate() != null)${String.valueOf(ev.getEndDate())}@endif
                      </td>
                      <td>${ev.getStatus() != null ? ev.getStatus().name() : ""}</td>
                      <td><a class="btn btn-sm btn-outline-primary" href="${contextPath}/event/${ev.getId()}">${msg.get("action.view")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="6" class="text-center text-muted">${msg.get("info.noEvent")}</td></tr>
                  @endfor
                </tbody>
              </table>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("statistics.topEventsByParticipants"),
      subheading=msg.get("statistics.topEventsCount", safeTopEvents.size()),
      content = @`
        <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">${msg.get("event.name")}</th>
                    <th scope="col">${msg.get("event.start")}</th>
                    <th scope="col">${msg.get("statistics.participantCount")}</th>
                    <th scope="col">${msg.get("event.status")}</th>
                    <th scope="col">${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(Object[] row : safeTopEvents)
                    <tr>
                      <td>${((com.github.gcolin.event.Event) row[0]).getId()}</td>
                      <td>${((com.github.gcolin.event.Event) row[0]).getName() != null ? ((com.github.gcolin.event.Event) row[0]).getName() : ""}</td>
                      <td>
                        @if(((com.github.gcolin.event.Event) row[0]).getStartDate() != null)${String.valueOf(((com.github.gcolin.event.Event) row[0]).getStartDate())}@endif
                      </td>
                      <td><span class="badge bg-success">${String.valueOf(row[1])}</span></td>
                      <td>${((com.github.gcolin.event.Event) row[0]).getStatus() != null ? ((com.github.gcolin.event.Event) row[0]).getStatus().name() : ""}</td>
                      <td><a class="btn btn-sm btn-outline-primary" href="${contextPath}/event/${((com.github.gcolin.event.Event) row[0]).getId()}">${msg.get("action.view")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="6" class="text-center text-muted">${msg.get("info.noEvent")}</td></tr>
                  @endfor
                </tbody>
              </table>
      `
    )
  `
)
