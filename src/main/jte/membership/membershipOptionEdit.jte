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
@param Integer seasonId
!{String optionPageTitle = option.getId() != null ? msg.get("page.membershipOptionEdit") : msg.get("page.membershipOptionNew");}
!{i18ntitle = optionPageTitle;}
!{title = optionPageTitle;}
!{String seasonParam = seasonId != null ? "?seasonId=" + seasonId : "";}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=optionPageTitle,
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/membership-option-admin${seasonParam}">${msg.get("breadcrumb.membershipOptions")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=optionPageTitle, icon="bi-toggles")

    @template.platform.adminPanel(content = @`
        <form method="post" action="${option.getId() != null ? contextPath + "/membership-option-admin/" + option.getId() : contextPath + "/membership-option-admin"}">
          @if(seasonId != null)
            <input type="hidden" name="seasonId" value="${seasonId}">
          @endif
          <div class="row g-3">
            <div class="col-12 col-md-6">
              <label for="opt_type" class="form-label">Type</label>
              <select id="opt_type" name="optionType" class="form-select" required>
                @for(var optType : optionTypes)
                  @if(option.getOptionType() != null && option.getOptionType() == optType)
                  <option value="${optType.name()}" selected>${msg.get("membershipOptionType." + optType)}</option>
                  @else
                  <option value="${optType.name()}">${msg.get("membershipOptionType." + optType)}</option>
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
                  <option value="${accessRule.name()}" selected>${msg.get("membershipOptionAccessRule." + accessRule)}</option>
                  @else
                  <option value="${accessRule.name()}">${msg.get("membershipOptionAccessRule." + accessRule)}</option>
                  @endif
                @endfor
              </select>
            </div>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <a class="btn  btn-secondary" href="${contextPath}/membership-option-admin${seasonParam}">Annuler</a>
            <button type="submit" class="btn btn-primary">${option.getId() != null ? "Enregistrer" : "Créer"}</button>
          </div>
        </form>
    `)
  `
)
