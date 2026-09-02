@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.Membership membership
@param com.github.gcolin.membership.MembershipStatus[] statuses
@param java.util.List<com.github.gcolin.membership.License> licenses
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.membershipNew");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.membership.License> safeLicenses = licenses != null ? licenses : java.util.List.of();}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
!{Integer effectiveSeasonId = seasonId;}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.membershipNew"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/membership">${msg.get("breadcrumb.memberships")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.membershipNew"), icon="bi-person-vcard")

    @template.platform.adminPanel(content = @`
        <form method="post" action="${contextPath}/membership">
          <div class="row g-3">
            <div class="col-12 col-md-6">
              <label for="membership_user" class="form-label">${msg.get("admin.membership.user")}</label>
              <input id="membership_user" type="text" name="user" class="form-control" required>
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_nrffe" class="form-label">${msg.get("admin.membership.nrFfe")}</label>
              <input id="membership_nrffe" type="text" name="nrFfe" class="form-control" required>
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_lastname" class="form-label">${msg.get("label.lastname")}</label>
              <input id="membership_lastname" type="text" name="lastname" class="form-control">
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_firstname" class="form-label">${msg.get("label.firstname")}</label>
              <input id="membership_firstname" type="text" name="firstname" class="form-control">
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_birthdate" class="form-label">${msg.get("player.birthdate")}</label>
              <input id="membership_birthdate" type="date" name="birthDate" class="form-control">
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_amount" class="form-label">${msg.get("admin.membership.amountCents")}</label>
              <input id="membership_amount" type="number" name="amountCents" class="form-control" min="0" required>
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_status" class="form-label">${msg.get("label.status")}</label>
              <select id="membership_status" name="status" class="form-select" required>
                @for(var memStatus : statuses)
                  <option value="${memStatus.name()}">${msg.get("clubRegister.membershipStatus." + memStatus)}</option>
                @endfor
              </select>
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_season" class="form-label">${msg.get("label.season")}</label>
              <select id="membership_season" name="seasonId" class="form-select" required>
                @for(var seasonItem : safeSeasons)
                  @if(!"0".equals(seasonItem.getValue()))
                    @if(effectiveSeasonId != null && effectiveSeasonId.toString().equals(seasonItem.getValue()))
                    <option value="${seasonItem.getValue()}" selected>${seasonItem.getLabel()}</option>
                    @else
                    <option value="${seasonItem.getValue()}">${seasonItem.getLabel()}</option>
                    @endif
                  @endif
                @endfor
              </select>
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_license_type" class="form-label">${msg.get("admin.membership.licenseType")}</label>
              <select id="membership_license_type" name="licenseType" class="form-select">
                @for(var lic : safeLicenses)
                  @if(com.github.gcolin.membership.Membership.DEFAULT_LICENSE_TYPE.equals(lic.getName()))
                  <option value="${lic.getName()}" selected>${msg.get("clubRegister.membershipOptions.license" + lic.getName())}</option>
                  @else
                  <option value="${lic.getName()}">${msg.get("clubRegister.membershipOptions.license" + lic.getName())}</option>
                  @endif
                @endfor
              </select>
            </div>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <a class="btn  btn-secondary" href="${contextPath}/membership">${msg.get("action.cancel")}</a>
            <button type="submit" class="btn btn-primary">${msg.get("action.createGeneric")}</button>
          </div>
        </form>
    `)
  `
)
