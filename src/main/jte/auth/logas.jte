@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<String> users
@param java.util.Set<String> admins
!{i18ntitle = msg.get("page.logas");}
!{title = i18ntitle;}
!{java.util.List<String> safeUsers = users != null ? users : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.logas"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.logas"), helpText=msg.get("help.logasDesc"), icon="bi-incognito")

    <div class="row g-4">
      <div class="col-lg-6">
        @template.platform.adminPanel(content = @`
            <form method="get" action="${contextPath}/auth-sim/logAs">
              <div class="mb-3">
                <label for="email" class="form-label">${msg.get("payment.email")}</label>
                <input type="email" class="form-control" id="email" name="email" required>
              </div>
              <div class="mb-3">
                <label for="name" class="form-label">${msg.get("logas.displayName")}</label>
                <input type="text" class="form-control" id="name" name="name" required>
              </div>
              <button type="submit" class="btn btn-primary">${msg.get("logas.submit")}</button>
            </form>
        `)
      </div>
      @if(!safeUsers.isEmpty())
      <div class="col-lg-6">
        @template.platform.adminPanel(
          heading=msg.get("logas.knownUsers"),
          content = @`
            <div class="list-group list-group-flush" style="max-height: 24rem; overflow-y: auto;">
              @for(var userEmail : safeUsers)
                <a class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                   href="${contextPath}/auth-sim/logAs?email=${java.net.URLEncoder.encode(String.valueOf(userEmail), java.nio.charset.StandardCharsets.UTF_8)}&name=${java.net.URLEncoder.encode(String.valueOf(userEmail), java.nio.charset.StandardCharsets.UTF_8)}">
                  ${userEmail}
                  @if(admins != null && admins.contains(userEmail))
                    <span class="badge bg-secondary">${msg.get("menu.admin")}</span>
                  @endif
                </a>
              @endfor
            </div>
          `
        )
      </div>
      @endif
    </div>
  `
)
