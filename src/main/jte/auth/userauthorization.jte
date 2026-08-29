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

          <div class="row g-3">
            <div class="col-12 col-lg-6">
              <label class="form-label" for="email">${msg.get("payment.email")}</label>
              <input class="form-control" type="email" id="email" name="email" autocomplete="email" placeholder="utilisateur@exemple.fr" required>
            </div>

            <div class="col-12 col-lg-6">
              <label class="form-label" for="permission">${msg.get("label.permission")}</label>
              <select class="form-select" id="permission" name="permission" required>
                @for(var perm : permissions)
                  <option value="${perm}">${msg.get("permission." + perm)}</option>
                @endfor
              </select>
            </div>

            <div class="col-12">
              <fieldset class="border rounded-3 p-3 mb-0">
                <legend class="float-none w-auto px-2 fs-6 mb-2">${msg.get("label.scope")}</legend>
                <p class="form-text mt-0 mb-3">${msg.get("help.userAuthorization.scope")}</p>
                <div class="row g-3">
                  <div class="col-12 col-md-6">
                    <label class="form-label" for="scopeType">${msg.get("label.scopeType")}</label>
                    <select class="form-select" id="scopeType" name="scopeType" required>
                      @for(var scopeType : scopeTypes)
                        <option value="${scopeType}">${msg.get("scopeType." + scopeType)}</option>
                      @endfor
                    </select>
                  </div>
                  <div class="col-12 col-md-6" id="scopeIdWrap">
                    <label class="form-label" for="scopeId">${msg.get("label.scopeId")}</label>
                    <input class="form-control" type="number" id="scopeId" name="scopeId" min="1" step="1" inputmode="numeric" disabled>
                    <div class="form-text">${msg.get("help.userAuthorization.scopeId")}</div>
                  </div>
                </div>
              </fieldset>
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
              <th scope="col">${msg.get("label.permission")}</th>
              <th scope="col">${msg.get("label.scopeType")}</th>
              <th scope="col">${msg.get("label.scopeId")}</th>
              <th scope="col">${msg.get("label.validUntil")}</th>
              <th scope="col">${msg.get("label.action")}</th>
            </tr>
          </thead>
          <tbody>
            @for(var authorization : safeAuthorizations)
              <tr>
                <td>${authorization.getId()}</td>
                <td><code class="text-body">${authorization.getEmail()}</code></td>
                <td>${msg.get("permission." + authorization.getPermission())}</td>
                <td>
                  @if(authorization.getScopeType() != null && authorization.getScopeType().name().equals("GLOBAL"))
                    <span class="badge text-bg-light border">${msg.get("userAuthorization.scopeGlobal")}</span>
                  @elseif(authorization.getScopeType() != null)
                    ${msg.get("scopeType." + authorization.getScopeType())}
                  @endif
                </td>
                <td>
                  @if(authorization.getScopeId() != null)
                    ${authorization.getScopeId()}
                  @else
                    <span class="text-body-secondary">—</span>
                  @endif
                </td>
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
  `,
  scripts = @`
    <script type="text/javascript">
      (function () {
        const scopeTypeSelect = document.getElementById('scopeType');
        const scopeIdInput = document.getElementById('scopeId');
        const scopeIdWrap = document.getElementById('scopeIdWrap');
        if (!scopeTypeSelect || !scopeIdInput || !scopeIdWrap) {
          return;
        }

        function syncScopeIdField() {
          const isGlobal = scopeTypeSelect.value === 'GLOBAL';
          scopeIdInput.disabled = isGlobal;
          scopeIdInput.required = !isGlobal;
          scopeIdWrap.classList.toggle('opacity-50', isGlobal);
          if (isGlobal) {
            scopeIdInput.value = '';
          }
        }

        scopeTypeSelect.addEventListener('change', syncScopeIdField);
        syncScopeIdField();
      })();
    </script>
  `
)
