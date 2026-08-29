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
!{i18ntitle = price.getId() != null ? msg.get("page.licensePriceEdit") : msg.get("page.licensePriceNew");}
!{title = i18ntitle;}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=price.getId() != null ? msg.get("action.edit") : msg.get("action.createGeneric"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/license-admin">${msg.get("menu.licenses")}</a></li>
    <li class="breadcrumb-item"><a href="${contextPath}/license-admin/${license.getId()}/price">${msg.get("breadcrumb.licensePrices")} ${license.getName()}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=price.getId() != null ? msg.get("page.licensePriceEdit") : msg.get("page.licensePriceNew"))

    @template.platform.adminPanel(content = @`
        <form method="post" action="${contextPath}/license-admin/price/save">
          <input type="hidden" name="licenseId" value="${license.getId()}">
          @if(price.getId() != null)
            <input type="hidden" name="id" value="${price.getId()}">
          @endif
          <div class="mb-3">
            <label for="category" class="form-label">${msg.get("label.category")}</label>
            <input type="text" class="form-control" id="category" name="category" value="${price.getCategory() != null ? price.getCategory() : ""}" required>
          </div>
          <div class="mb-3">
            <label for="priceCents" class="form-label">${msg.get("label.price")} (€)</label>
            <input type="number" class="form-control" id="priceCents" name="priceCents" step="0.01" value="@if(price.getPriceCents() != null)${String.format(java.util.Locale.US, "%.2f", price.getPriceCents() / 100.0)}@endif" required>
            <div class="form-text">${msg.get("licensePrice.centsHint")}</div>
          </div>
          <div class="d-flex flex-wrap gap-2">
            <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
            @if(price.getId() != null)
              <button type="button" class="btn btn-outline-danger" onclick="removePrice(${price.getId()}, ${license.getId()})">${msg.get("action.delete")}</button>
            @endif
            <a href="${contextPath}/license-admin/${license.getId()}/price" class="btn  btn-secondary">${msg.get("action.cancel")}</a>
          </div>
        </form>
    `)
  `,
  scripts = @`
    <script type="text/javascript">
      function removePrice(priceId, licenseId) {
        if (!confirm('${msg.get("licensePrice.confirmDelete")}')) {
          return;
        }
        const form = document.createElement('form');
        form.method = 'post';
        form.action = '${contextPath}/license-admin/price/save';
        form.innerHTML = '<input type="hidden" name="id" value="' + priceId + '">' +
                         '<input type="hidden" name="licenseId" value="' + licenseId + '">' +
                         '<input type="hidden" name="toRemove" value="true">';
        document.body.appendChild(form);
        form.submit();
      }
    </script>
  `
)
