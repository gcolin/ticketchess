@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<com.github.gcolin.auth.UserAuthorization> authorizations
@param java.util.List<com.github.gcolin.auth.PermissionCode> permissions
@param java.util.List<com.github.gcolin.auth.AuthorizationScopeType> scopeTypes
!{i18ntitle = msg.get("page.userAuthorizations");}
!{title = i18ntitle;}
!{page = "userauthorizations";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.userAuthorizations")}</li>
        </ol>
      </nav>

      <div class="card mb-3">
        <div class="card-header"><strong>${msg.get("heading.userAuthorizations")}</strong></div>
        <div class="card-body">
          <p class="text-body-secondary">${msg.get("help.userAuthorizationsDesc")}</p>
          <form method="post" action="${contextPath}/user-authorization" class="row g-3">
            <input type="hidden" name="toRemove" value="false">

            <div class="col-md-4">
              <label class="form-label" for="email">${msg.get("payment.email")}</label>
              <input class="form-control" type="email" id="email" name="email" required>
            </div>

            <div class="col-md-3">
              <label class="form-label" for="permission">${msg.get("label.permission")}</label>
              <select class="form-select" id="permission" name="permission" required>
                @for(var perm : permissions)
                  <option value="${perm}">${msg.get("permission." + perm)}</option>
                @endfor
              </select>
            </div>

            <div class="col-md-2">
              <label class="form-label" for="scopeType">${msg.get("label.scopeType")}</label>
              <select class="form-select" id="scopeType" name="scopeType" required>
                @for(var scopeType : scopeTypes)
                  <option value="${scopeType}">${msg.get("scopeType." + scopeType)}</option>
                @endfor
              </select>
            </div>

            <div class="col-md-1">
              <label class="form-label" for="scopeId">${msg.get("label.scopeId")}</label>
              <input class="form-control" type="number" id="scopeId" name="scopeId" min="1">
            </div>

            <div class="col-md-2">
              <label class="form-label" for="validUntil">${msg.get("label.validUntil")}</label>
              <input class="form-control" type="datetime-local" id="validUntil" name="validUntil">
            </div>

            <div class="col-12 d-flex justify-content-end">
              <button type="submit" class="btn btn-primary">${msg.get("action.createGeneric")}</button>
            </div>
          </form>
        </div>
      </div>

      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <strong>${msg.get("heading.userAuthorizationsList")}</strong>
          <span class="badge bg-secondary">${authorizations.size()}</span>
        </div>
        <div class="card-body">
          @if((authorizations != null && !authorizations.isEmpty()))
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>${msg.get("payment.email")}</th>
                    <th>${msg.get("label.permission")}</th>
                    <th>${msg.get("label.scopeType")}</th>
                    <th>${msg.get("label.scopeId")}</th>
                    <th>${msg.get("label.validUntil")}</th>
                    <th>${msg.get("label.action")}</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var authorization : authorizations)
                    <tr>
                      <td>${authorization.getId()}</td>
                      <td>${authorization.getEmail()}</td>
                      <td>${authorization.getPermission()}</td>
                      <td>${authorization.getScopeType()}</td>
                      <td>${authorization.getScopeId() != null ? String.valueOf(authorization.getScopeId()) : ""}</td>
                      <td>${authorization.getValidUntil() != null ? String.valueOf(authorization.getValidUntil()) : ""}</td>
                      <td>
                        <form method="post" action="${contextPath}/user-authorization" class="d-inline">
                          <input type="hidden" name="id" value="${authorization.getId()}">
                          <input type="hidden" name="toRemove" value="true">
                          <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('${msg.get("sub.confirmDelete")}');">
                            ${msg.get("action.remove")}
                          </button>
                        </form>
                      </td>
                    </tr>
                  @endfor
                </tbody>
              </table>
            </div>
          @else
            <p class="text-body-secondary mb-0">${msg.get("info.noData")}</p>
          @endif
        </div>
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
  </body>
</html>