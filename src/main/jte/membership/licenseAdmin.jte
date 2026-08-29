@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.License license
@param java.util.List<com.github.gcolin.membership.License> licenses
@param com.github.gcolin.membership.LicensePrice price
@param Object type
!{i18ntitle = msg.get("page.licenses");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.membership.License> safeLicenses = licenses != null ? licenses : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("menu.licenses"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.licenses"), helpText=msg.get("help.licenses"), icon="bi-card-list")

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <a class="btn btn-success" href="${contextPath}/license-admin/new">${msg.get("action.createGeneric")}</a>
      `)
      <table class="table table-striped align-middle mb-0 mt-3">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("label.name")}</th>
            <th scope="col">${msg.get("label.action")}</th>
          </tr>
        </thead>
        <tbody>
          @for(var lic : safeLicenses)
            <tr>
              <td>${lic.getId()}</td>
              <td>${lic.getName()}</td>
              <td>
                <div class="d-flex gap-2">
                  <a href="${contextPath}/license-admin/${lic.getId()}" class="btn btn-sm btn-outline-primary">${msg.get("action.edit")}</a>
                  <a href="${contextPath}/license-admin/${lic.getId()}/price" class="btn btn-sm  btn-secondary">${msg.get("action.prices")}</a>
                </div>
              </td>
            </tr>
          @else
            <tr>
              <td colspan="3" class="text-center text-body-secondary">${msg.get("info.noLicense")}</td>
            </tr>
          @endfor
        </tbody>
      </table>
    `)
  `
)
