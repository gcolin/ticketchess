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
@param java.util.List<com.github.gcolin.auth.RoleCode> roles
@param java.util.List<com.github.gcolin.auth.AuthorizationScopeType> scopeTypes
!{i18ntitle = msg.get("page.userAuthorizations");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.auth.UserAuthorization> safeAuthorizations = authorizations != null ? authorizations : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.userAuthorizations"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.userAuthorizations"), helpText=msg.get("help.userAuthorizationsDesc"), icon="bi-shield-lock")

    @template.platform.adminPanel(
      heading=msg.get("heading.userAuthorizationNew"),
      content = @`
        <form method="post" action="${contextPath}/user-authorization" id="userAuthorizationForm">
          <input type="hidden" name="toRemove" value="false">
          <input type="hidden" name="scopeType" value="GLOBAL">

          <div class="row g-3">
            <div class="col-12 col-lg-6">
              <label class="form-label" for="email">${msg.get("payment.email")}</label>
              <input class="form-control" type="email" id="email" name="email" autocomplete="email" placeholder="utilisateur@exemple.fr" required>
            </div>

            <div class="col-12 col-lg-6">
              <label class="form-label" for="role">${msg.get("label.role")}</label>
              <select class="form-select" id="role" name="role" required>
                @for(var roleOption : roles)
                  <option value="${roleOption}">${msg.get("role." + roleOption)}</option>
                @endfor
              </select>
            </div>

            <div class="col-12 col-md-6">
              <label class="form-label" for="validUntil">${msg.get("label.validUntil")}</label>
              <input class="form-control" type="datetime-local" id="validUntil" name="validUntil">
              <div class="form-text">${msg.get("help.userAuthorization.validUntil")}</div>
            </div>
          </div>

          <div class="d-flex justify-content-end mt-4">
            <button type="submit" class="btn btn-success">
              <i class="bi bi-plus-lg me-1"></i>${msg.get("action.createGeneric")}
            </button>
          </div>
        </form>
      `
    )

    @template.platform.adminPanel(content = @`
      <div class="d-flex justify-content-between align-items-center gap-2 mb-3">
        <h2 class="h5 mb-0">${msg.get("heading.userAuthorizationsList")}</h2>
        <span class="badge bg-secondary">${safeAuthorizations.size()}</span>
      </div>

      @if(!safeAuthorizations.isEmpty())
        <table class="table table-striped align-middle mb-0 mt-3">
          <thead>
            <tr>
              <th scope="col">#</th>
              <th scope="col">${msg.get("payment.email")}</th>
              <th scope="col">${msg.get("label.role")}</th>
              <th scope="col">${msg.get("label.validUntil")}</th>
              <th scope="col">${msg.get("label.action")}</th>
            </tr>
          </thead>
          <tbody>
            @for(var authorization : safeAuthorizations)
              <tr>
                <td>${authorization.getId()}</td>
                <td><code class="text-body">${authorization.getEmail()}</code></td>
                <td>${msg.get("role." + authorization.getRole())}</td>
                <td>
                  @if(authorization.getValidUntil() != null)
                    ${authorization.getValidUntil().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}
                  @else
                    <span class="badge text-bg-success">${msg.get("userAuthorization.permanent")}</span>
                  @endif
                </td>
                <td>
                  <form method="post" action="${contextPath}/user-authorization" class="m-0">
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
      @else
        <p class="text-body-secondary mb-0">${msg.get("info.noData")}</p>
      @endif
    `)
  `
)
