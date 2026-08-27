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
@param Object p
!{i18ntitle = msg.get("page.statistics");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "statistics";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("page.statistics")}</li>
      </ol>
    </nav>

    <div class="row g-3">

      <!-- Total des paiements -->
      <div class="col-12">
        <div class="card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>${msg.get("statistics.totalPayments")}</strong>
          </div>
          <div class="card-body">
            <h3 class="display-6 text-success">${String.format(java.util.Locale.US, "%.2f", totalPayments)} €</h3>
          </div>
        </div>
      </div>

      <!-- Total des paiements + souscriptions non payées -->
      <div class="col-12">
        <div class="card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>${msg.get("statistics.totalPayments")} + ${msg.get("statistics.unpaidSubscriptions")}</strong>
          </div>
          <div class="card-body">
            <h3 class="display-6 text-info">${String.format(java.util.Locale.US, "%.2f", totalPayments + unpaidPlayerSubscriptions)} €</h3>
          </div>
        </div>
      </div>

      <!-- 10 évènements les plus proches -->
      <div class="col-12">
        <div class="card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>${msg.get("statistics.closestEvents")}</strong>
            <span class="badge bg-primary">${closestEvents.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>${msg.get("event.name")}</th>
                    <th>${msg.get("event.start")}</th>
                    <th>${msg.get("event.end")}</th>
                    <th>${msg.get("event.status")}</th>
                    <th>${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var ev : closestEvents)
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
                      <td><a class="btn btn-outline-primary btn-sm" href="${contextPath}/event/${ev.getId()}">${msg.get("action.view")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="6" class="text-center text-muted">${msg.get("info.noEvent")}</td></tr>
                  @endfor
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 10 évènements avec le plus de participants -->
      <div class="col-12 mb-3">
        <div class="card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>${msg.get("statistics.topEventsByParticipants")}</strong>
            <span class="badge bg-info">${topEvents.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>${msg.get("event.name")}</th>
                    <th>${msg.get("event.start")}</th>
                    <th>${msg.get("statistics.participantCount")}</th>
                    <th>${msg.get("event.status")}</th>
                    <th>${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(Object[] row : topEvents)
                    <tr>
                      <td>${((com.github.gcolin.event.Event) row[0]).getId()}</td>
                      <td>${((com.github.gcolin.event.Event) row[0]).getName() != null ? ((com.github.gcolin.event.Event) row[0]).getName() : ""}</td>
                      <td>
                        @if(((com.github.gcolin.event.Event) row[0]).getStartDate() != null)${String.valueOf(((com.github.gcolin.event.Event) row[0]).getStartDate())}@endif
                      </td>
                      <td><span class="badge bg-success">${String.valueOf(row[1])}</span></td>
                      <td>${((com.github.gcolin.event.Event) row[0]).getStatus() != null ? ((com.github.gcolin.event.Event) row[0]).getStatus().name() : ""}</td>
                      <td><a class="btn btn-outline-primary btn-sm" href="${contextPath}/event/${((com.github.gcolin.event.Event) row[0]).getId()}">${msg.get("action.view")}</a></td>
                    </tr>
                  @else
                    <tr><td colspan="6" class="text-center text-muted">${msg.get("info.noEvent")}</td></tr>
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