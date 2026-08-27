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
@param com.github.gcolin.membership.MembershipOptionType[] optionTypes
@param java.util.List<com.github.gcolin.membership.License> licenses
@param com.github.gcolin.membership.MembershipOption option
!{i18ntitle = option.getId() != null ? msg.get("page.membershipOptionEdit") : msg.get("page.membershipOptionNew");}
!{title = i18ntitle;}
!{page = "admin";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3 mt-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/membership-option-admin">${msg.get("breadcrumb.membershipOptions")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${i18ntitle}</li>
      </ol>
    </nav>

    <div class="bg-body rounded p-3">
      <form method="post" action="${option.getId() != null ? contextPath + "/membership-option-admin/" + option.getId() : contextPath + "/membership-option-admin"}">
        <div class="row g-3">
          <div class="col-12 col-md-6">
            <label for="opt_type" class="form-label">Type</label>
            <select id="opt_type" name="optionType" class="form-select" required>
              @for(var optType : optionTypes)
                @if(option.getOptionType() != null && option.getOptionType() == optType)
                <option value="${optType.name()}" selected>${optType.name()}</option>
                @else
                <option value="${optType.name()}">${optType.name()}</option>
                @endif
              @endfor
            </select>
          </div>

          <div class="col-12 col-md-6">
            <label for="opt_license" class="form-label">Licence (optionnelle)</label>
            <select id="opt_license" name="licenseId" class="form-select">
              <option value="">-- Toutes les licences --</option>
              @for(var lic : licenses)
                @if(option.getLicense() != null && option.getLicense().getId() != null && option.getLicense().getId().equals(lic.getId()))
                <option value="${lic.getId()}" selected>${lic.getName()}</option>
                @else
                <option value="${lic.getId()}">${lic.getName()}</option>
                @endif
              @endfor
            </select>
          </div>

          <div class="col-12 col-md-6">
            <label for="opt_value" class="form-label">Valeur</label>
            <input id="opt_value" type="text" name="optionValue" class="form-control" value="${option.getOptionValue() != null ? option.getOptionValue() : ""}" required>
          </div>

          <div class="col-12 col-md-6">
            <label for="opt_amount" class="form-label">Montant (cents)</label>
            <input id="opt_amount" type="number" name="amountCents" class="form-control" min="0" value="${option.getAmountCents() != null ? option.getAmountCents() : 0}">
          </div>

          <div class="col-12 col-md-6">
            <label for="opt_access_rule" class="form-label">Accessibilite</label>
            <select id="opt_access_rule" name="accessRule" class="form-select">
              @for(var accessRule : accessRules)
                @if(option.getAccessRule() != null && option.getAccessRule() == accessRule)
                <option value="${accessRule.name()}" selected>
                  @if(accessRule.name().equals("ALL"))Tous@endif
                  @if(accessRule.name().equals("YOUNG_ONLY"))Young uniquement@endif
                  @if(accessRule.name().equals("NON_YOUNG_ONLY"))Non young uniquement@endif
                  @if(accessRule.name().equals("ADMIN"))Admin uniquement@endif
                </option>
                @else
                <option value="${accessRule.name()}">
                  @if(accessRule.name().equals("ALL"))Tous@endif
                  @if(accessRule.name().equals("YOUNG_ONLY"))Young uniquement@endif
                  @if(accessRule.name().equals("NON_YOUNG_ONLY"))Non young uniquement@endif
                  @if(accessRule.name().equals("ADMIN"))Admin uniquement@endif
                </option>
                @endif
              @endfor
            </select>
          </div>
        </div>

        <div class="d-flex justify-content-end gap-2 mt-4">
          <a class="btn btn-outline-secondary" href="${contextPath}/membership-option-admin">Annuler</a>
          <button type="submit" class="btn btn-primary">${option.getId() != null ? "Enregistrer" : "Créer"}</button>
        </div>
      </form>
    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>
