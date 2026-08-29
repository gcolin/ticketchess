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
@param com.github.gcolin.membership.LicensePrice price
@param java.util.List<com.github.gcolin.membership.LicensePrice> prices
!{i18ntitle = msg.get("page.licensePrices");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.membership.LicensePrice> safePrices = prices != null ? prices : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("breadcrumb.licensePrices"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/license-admin">${msg.get("menu.licenses")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.licensePrices", license.getName()), helpText=msg.get("help.licensePrices"))

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <div class="d-flex gap-2">
          <a href="${contextPath}/license-admin/${license.getId()}/price/new" class="btn btn-success">${msg.get("action.createGeneric")}</a>
          <a href="${contextPath}/license-admin" class="btn  btn-secondary">${msg.get("action.cancel")}</a>
        </div>
      `)
      <table class="table table-striped align-middle mb-0 mt-3">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("label.category")}</th>
            <th scope="col">${msg.get("label.price")}</th>
            <th scope="col">${msg.get("label.action")}</th>
          </tr>
        </thead>
        <tbody>
          @for(var lp : safePrices)
            <tr>
              <td>${lp.getId()}</td>
              <td>${lp.getCategory()}</td>
              <td>${String.format(java.util.Locale.FRANCE, "%.2f", lp.getPriceCents() / 100.0)} €</td>
              <td>
                <a href="${contextPath}/license-admin/${license.getId()}/price/${lp.getId()}/edit" class="btn btn-sm btn-outline-primary">${msg.get("action.edit")}</a>
              </td>
            </tr>
          @else
            <tr>
              <td colspan="4" class="text-center text-body-secondary">${msg.get("info.noLicensePrice")}</td>
            </tr>
          @endfor
        </tbody>
      </table>
    `)
  `
)
