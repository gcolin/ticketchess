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
!{java.util.List<java.util.Map<String, Object>> safeSessions = sessions != null ? sessions : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.loggedUsers"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.loggedUsers"), helpText=msg.get("help.loggedUsersDesc"), icon="bi-person-lines-fill")

    @template.platform.adminPanel(
      heading=msg.get("heading.loggedUsers"),
      subheading=msg.get("loggedUsers.sessionCount", sessionCount),
      content = @`
      @if(!safeSessions.isEmpty())
        <table class="table table-striped align-middle mb-0">
          <thead>
            <tr>
              <th scope="col">${msg.get("payment.email")}</th>
              <th scope="col">${msg.get("logas.displayName")}</th>
              <th scope="col">${msg.get("loggedUsers.lastSeen")}</th>
              <th scope="col">${msg.get("loggedUsers.session")}</th>
              <th scope="col"></th>
            </tr>
          </thead>
          <tbody>
            @for(var session : safeSessions)
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
      @else
        <p class="text-body-secondary mb-0">${msg.get("loggedUsers.none")}</p>
      @endif
      `
    )
  `
)
