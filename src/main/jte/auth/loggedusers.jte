@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param int sessionCount
@param java.util.List<java.util.Map<String, Object>> sessions
!{i18ntitle = msg.get("page.loggedUsers");}
!{title = i18ntitle;}
!{page = "loggedusers";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.loggedUsers")}</li>
        </ol>
      </nav>

      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <strong>${msg.get("heading.loggedUsers")}</strong>
          <span class="badge bg-secondary">${sessionCount}</span>
        </div>
        <div class="card-body">
          <p class="text-body-secondary">${msg.get("help.loggedUsersDesc")}</p>
          @if((sessions != null && !sessions.isEmpty()))
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>${msg.get("payment.email")}</th>
                    <th>${msg.get("logas.displayName")}</th>
                    <th>${msg.get("loggedUsers.lastSeen")}</th>
                    <th>${msg.get("loggedUsers.session")}</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  @for(var session : sessions)
                    <tr>
                      <td>${String.valueOf(session.get("email"))}</td>
                      <td>${session.get("username") != null ? String.valueOf(session.get("username")) : ""}</td>
                      <td>${String.valueOf(session.get("lastSeen"))}</td>
                      <td><code>${String.valueOf(session.get("sessionIdShort"))}</code></td>
                      <td>
                        @if(Boolean.TRUE.equals(session.get("admin")))
                          <span class="badge bg-secondary">${msg.get("menu.admin")}</span>
                        @endif
                      </td>
                    </tr>
                  @endfor
                </tbody>
              </table>
            </div>
          @else
            <p class="text-body-secondary mb-0">${msg.get("loggedUsers.none")}</p>
          @endif
        </div>
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
  </body>
</html>