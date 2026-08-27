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
!{page = "logas";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.logas")}</li>
        </ol>
      </nav>
      <div class="row g-4">
        <div class="col-lg-6">
          <div class="card">
            <div class="card-body">
              <h1>${msg.get("heading.logas")}</h1>
              <p class="text-body-secondary">${msg.get("help.logasDesc")}</p>
              <form method="get" action="${contextPath}/auth-sim/logAs" class="mt-4">
                <div class="mb-3">
                  <label for="email" class="form-label">${msg.get("payment.email")}</label>
                  <input type="email" class="form-control" id="email" name="email" required>
                </div>
                <div class="mb-3">
                  <label for="name" class="form-label">${msg.get("logas.displayName")}</label>
                  <input type="text" class="form-control" id="name" name="name" required>
                </div>
                <div class="mb-3 form-check">
                  <input type="checkbox" class="form-check-input" id="admin" name="admin" value="true">
                  <label class="form-check-label" for="admin">${msg.get("logas.asAdmin")}</label>
                </div>
                <button type="submit" class="btn btn-primary">${msg.get("logas.submit")}</button>
              </form>
            </div>
          </div>
        </div>
        @if((users != null && !users.isEmpty()))
        <div class="col-lg-6">
          <div class="card">
            <div class="card-header">${msg.get("logas.knownUsers")}</div>
            <div class="card-body p-0">
              <div class="list-group list-group-flush" style="max-height: 24rem; overflow-y: auto;">
                @for(var userEmail : users)
                  <a class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                     href="${contextPath}/auth-sim/logAs?email=${java.net.URLEncoder.encode(String.valueOf(userEmail), java.nio.charset.StandardCharsets.UTF_8)}&name=${java.net.URLEncoder.encode(String.valueOf(userEmail), java.nio.charset.StandardCharsets.UTF_8)}">
                    ${userEmail}
                    @if(admins != null && admins.contains(userEmail))
                      <span class="badge bg-secondary">${msg.get("menu.admin")}</span>
                    @endif
                  </a>
                @endfor
              </div>
            </div>
          </div>
        </div>
        @endif
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
  </body>
</html>