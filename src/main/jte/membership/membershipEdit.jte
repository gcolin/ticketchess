@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.Membership membership
@param com.github.gcolin.membership.MembershipStatus[] statuses
@param java.util.List<com.github.gcolin.membership.MembershipOptionSubscription> membershipOptions
@param java.util.List<com.github.gcolin.membership.MembershipOption> availableOptions
@param java.util.List<com.github.gcolin.membership.License> licenses
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.membershipEdit");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.membership.License> safeLicenses = licenses != null ? licenses : java.util.List.of();}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
!{String effectiveLicenseType = membership.getLicenseType() != null && !membership.getLicenseType().isBlank() ? membership.getLicenseType() : com.github.gcolin.membership.Membership.DEFAULT_LICENSE_TYPE;}
!{Integer effectiveSeasonId = seasonId != null ? seasonId : (membership.getSeason() != null && membership.getSeason().getId() != null ? membership.getSeason().getId() : null);}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.membershipEdit"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/membership">${msg.get("breadcrumb.memberships")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.membershipEdit"), icon="bi-person-vcard")

    @template.platform.adminPanel(content = @`
        <form method="post" action="${contextPath}/membership/${membership.getId()}">
          <div class="row g-3">
            <div class="col-12 col-md-6">
              <label for="membership_user" class="form-label">${msg.get("admin.membership.user")}</label>
              <input id="membership_user" type="text" name="user" class="form-control" value="${membership.getUser() != null ? membership.getUser() : ""}" required>
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_nrffe" class="form-label">${msg.get("admin.membership.nrFfe")}</label>
              <input id="membership_nrffe" type="text" name="nrFfe" class="form-control" value="${membership.getNrFfe() != null ? membership.getNrFfe() : ""}">
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_lastname" class="form-label">${msg.get("label.lastname")}</label>
              <input id="membership_lastname" type="text" name="lastname" class="form-control" value="${membership.getLastname() != null ? membership.getLastname() : ""}">
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_firstname" class="form-label">${msg.get("label.firstname")}</label>
              <input id="membership_firstname" type="text" name="firstname" class="form-control" value="${membership.getFirstname() != null ? membership.getFirstname() : ""}">
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_birthdate" class="form-label">${msg.get("player.birthdate")}</label>
              <input id="membership_birthdate" type="date" name="birthDate" class="form-control" value="${membership.getBirthDate() != null ? membership.getBirthDate() : ""}">
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_amount" class="form-label">${msg.get("admin.membership.amountCents")}</label>
              <input id="membership_amount" type="number" name="amountCents" class="form-control" min="0" value="${membership.getAmountCents()}" required>
            </div>

            <div class="col-12 col-md-6">
              <label for="membership_status" class="form-label">${msg.get("label.status")}</label>
              <select id="membership_status" name="status" class="form-select" required>
                @for(var memStatus : statuses)
                  @if(membership.getStatus() != null && membership.getStatus() == memStatus)
                  <option value="${memStatus.name()}" selected>${msg.get("clubRegister.membershipStatus." + memStatus)}</option>
                  @else
                  <option value="${memStatus.name()}">${msg.get("clubRegister.membershipStatus." + memStatus)}</option>
                  @endif
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
                  @if(effectiveLicenseType.equals(lic.getName()))
                  <option value="${lic.getName()}" selected>${msg.get("clubRegister.membershipOptions.license" + lic.getName())}</option>
                  @else
                  <option value="${lic.getName()}">${msg.get("clubRegister.membershipOptions.license" + lic.getName())}</option>
                  @endif
                @endfor
              </select>
            </div>
          </div>

          <div class="mt-4">
            <h5 class="mb-3">${msg.get("clubRegister.myMemberships.options")}</h5>
            @if(membershipOptions != null && membershipOptions.size() > 0)
                <table class="table table-sm table-hover mb-0">
                  <thead class="table-light">
                    <tr>
                      <th scope="col">Type</th>
                      <th scope="col">Valeur</th>
                      <th scope="col">Montant</th>
                      <th scope="col">${msg.get("label.action")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for(var mos : membershipOptions)
                      <tr>
                        <td>${mos.getMembershipOption() != null && mos.getMembershipOption().getOptionType() != null ? mos.getMembershipOption().getOptionType().name() : ""}</td>
                        <td>${mos.getMembershipOption() != null && mos.getMembershipOption().getOptionValue() != null ? mos.getMembershipOption().getOptionValue() : ""}</td>
                        <td>${(mos.getMembershipOption() != null && mos.getMembershipOption().getAmountCents() != null ? mos.getMembershipOption().getAmountCents() : 0) / 100.0} €</td>
                        <td>
                          <a class="btn btn-sm btn-outline-danger" href="${contextPath}/membership/${membership.getId()}/option/${mos.getId()}/remove">${msg.get("action.remove")}</a>
                        </td>
                      </tr>
                    @endfor
                  </tbody>
                </table>
            @else
              <p class="text-muted">Aucune option associée</p>
            @endif

            <div class="mt-3">
              <button type="button" class="btn btn-success btn-sm" data-bs-toggle="modal" data-bs-target="#addOptionModal">${msg.get("action.addMembershipOption")}</button>
            </div>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn  btn-secondary" data-bs-toggle="modal" data-bs-target="#sendMailModal">Envoyer mail</button>
            <a class="btn  btn-secondary" href="${contextPath}/membership">${msg.get("action.cancel")}</a>
            <button type="submit" class="btn btn-primary" id="membership-save">${msg.get("action.save")}</button>
          </div>
        </form>
    `)

    <div class="modal fade" id="sendMailModal" tabindex="-1" aria-labelledby="sendMailModalLabel" aria-hidden="true">
      <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="sendMailModalLabel">Aperçu de l'email</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
          </div>
          <div class="modal-body">
            <p class="text-muted mb-2">Destinataire : <strong>${membership.getUser() != null ? membership.getUser() : ""}</strong></p>
            <p class="text-muted mb-3">Objet : <strong>Votre inscription a bien été prise en compte</strong></p>
            <iframe
              id="membershipMailPreview"
              title="Aperçu de l'email"
              class="w-100 border rounded"
              style="min-height: 420px; background-color: #f4f6f8;"
              src="${contextPath}/membership/${membership.getId()}/mail-preview"
            ></iframe>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn  btn-secondary" data-bs-dismiss="modal">Annuler</button>
            <form method="post" action="${contextPath}/membership/${membership.getId()}/send-mail" class="d-inline">
              <button type="submit" class="btn btn-primary">Envoyer l'email</button>
            </form>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" id="addOptionModal" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">${msg.get("action.addMembershipOption")}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <form method="post" action="${contextPath}/membership/${membership.getId()}/option">
            <div class="modal-body">
              <div class="mb-3">
                <label for="option_select" class="form-label">Sélectionner une option</label>
                <select id="option_select" name="optionId" class="form-select" required>
                  <option value="">-- Sélectionner --</option>
                  @if(availableOptions != null)
                    @for(var availOpt : availableOptions)
                      <option value="${availOpt.getId()}">${availOpt.getOptionValue()} (${(availOpt.getAmountCents() != null ? availOpt.getAmountCents() : 0) / 100.0} €)</option>
                    @endfor
                  @endif
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn  btn-secondary" data-bs-dismiss="modal">Annuler</button>
              <button type="submit" class="btn btn-primary">Ajouter</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  scripts = @`
    <script>
      (function () {
        var modal = document.getElementById('sendMailModal');
        var frame = document.getElementById('membershipMailPreview');
        if (!modal || !frame) {
          return;
        }
        var previewUrl = frame.getAttribute('src');
        modal.addEventListener('show.bs.modal', function () {
          frame.src = previewUrl + (previewUrl.indexOf('?') >= 0 ? '&' : '?') + 't=' + Date.now();
        });
      })();
    </script>
  `
)
