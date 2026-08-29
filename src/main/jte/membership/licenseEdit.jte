@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.MembershipOptionAccessRule[] accessRules
@param com.github.gcolin.membership.License license
@param com.github.gcolin.membership.MembershipOption option
@param com.github.gcolin.membership.LicensePrice price
!{i18ntitle = license.getId() != null ? msg.get("page.licenseEdit") : msg.get("page.licenseNew");}
!{title = i18ntitle;}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=license.getId() != null ? msg.get("action.edit") : msg.get("action.createGeneric"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/license-admin">${msg.get("menu.licenses")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=license.getId() != null ? msg.get("page.licenseEdit") : msg.get("page.licenseNew"))

    @template.platform.adminPanel(content = @`
        <form method="post" action="${contextPath}/license-admin">
          @if(license.getId() != null)
            <input type="hidden" name="id" value="${license.getId()}">
          @endif
          <div class="mb-3">
            <label for="name" class="form-label">${msg.get("label.name")}</label>
            <input type="text" class="form-control" id="name" name="name" value="${license.getName() != null ? license.getName() : ""}" required>
          </div>
          <div class="mb-3">
            <label for="accessRule" class="form-label">${msg.get("label.accessibility")}</label>
            <select class="form-select" id="accessRule" name="accessRule" required>
              @for(var accessRule : accessRules)
                @if(license.getAccessRule() != null && license.getAccessRule() == accessRule)
                  <option value="${accessRule.name()}" selected>${msg.get("membershipOptionAccessRule." + accessRule)}</option>
                @else
                  <option value="${accessRule.name()}">${msg.get("membershipOptionAccessRule." + accessRule)}</option>
                @endif
              @endfor
            </select>
          </div>
          @if(license.getId() != null)
            <div class="mb-3">
              <a href="${contextPath}/license-admin/${license.getId()}/price" class="btn  btn-secondary">${msg.get("action.managePrices")}</a>
            </div>
          @endif
          <div class="d-flex flex-wrap gap-2">
            <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
            @if(license.getId() != null)
              <button type="button" class="btn btn-outline-danger" onclick="removeLicense(${license.getId()})">${msg.get("action.delete")}</button>
            @endif
            <a href="${contextPath}/license-admin" class="btn  btn-secondary">${msg.get("action.cancel")}</a>
          </div>
        </form>
    `)
  `,
  scripts = @`
    <script type="text/javascript">
      function removeLicense(id) {
        if (!confirm('${msg.get("license.confirmDelete")}')) {
          return;
        }
        const form = document.createElement('form');
        form.method = 'post';
        form.action = '${contextPath}/license-admin';
        form.innerHTML = '<input type="hidden" name="id" value="' + id + '">' +
                         '<input type="hidden" name="toRemove" value="true">';
        document.body.appendChild(form);
        form.submit();
      }
    </script>
  `
)
