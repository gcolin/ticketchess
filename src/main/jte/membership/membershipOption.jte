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
@param java.util.List<com.github.gcolin.membership.MembershipOption> options
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.membershipOptions");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
!{java.util.List<com.github.gcolin.membership.MembershipOption> safeOptions = options != null ? options : java.util.List.of();}
!{String seasonParam = seasonId != null ? "?seasonId=" + seasonId : "";}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("breadcrumb.membershipOptions"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.membershipOptions"), helpText=msg.get("help.membershipOptions"), icon="bi-toggles")

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <form method="get" action="${contextPath}/membership-option-admin" class="d-flex align-items-center mb-0">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="membership_option_season", autoSubmit=true, inline=true)
        </form>
        <a class="btn btn-success ms-md-auto" href="${contextPath}/membership-option-admin/new${seasonParam}">${msg.get("action.addMembershipOption")}</a>
      `)
      <table class="table table-striped align-middle mb-0 mt-3">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("label.type")}</th>
            <th scope="col">${msg.get("label.season")}</th>
            <th scope="col">${msg.get("label.value")}</th>
            <th scope="col">${msg.get("label.license")}</th>
            <th scope="col">${msg.get("label.amount")} (€)</th>
            <th scope="col">${msg.get("label.accessibility")}</th>
            <th scope="col">${msg.get("label.action")}</th>
          </tr>
        </thead>
        <tbody>
          @for(var opt : safeOptions)
            <tr>
              <td>${opt.getId()}</td>
              <td>${opt.getOptionType() != null ? opt.getOptionType().name() : ""}</td>
              <td>${opt.getSeason() != null && opt.getSeason().getName() != null ? opt.getSeason().getName() : ""}</td>
              <td>${opt.getOptionValue() != null ? opt.getOptionValue() : ""}</td>
              <td>@if(opt.getLicense() != null)${msg.get("label.license")} ${opt.getLicense().getName()}@else${msg.get("label.allLicenses")}@endif</td>
              <td>${(opt.getAmountCents() != null ? opt.getAmountCents() : 0) / 100.0} €</td>
              <td>
                @if(opt.getAccessRule() != null)
                  ${msg.get("membershipOptionAccessRule." + opt.getAccessRule())}
                @endif
              </td>
              <td>
                <div class="d-flex gap-2">
                  <a class="btn btn-sm btn-outline-primary" href="${contextPath}/membership-option-admin/${opt.getId()}/edit${seasonParam}">${msg.get("action.edit")}</a>
                  <form method="post" class="m-0" action="${contextPath}/membership-option-admin/${opt.getId()}/delete" onsubmit="return confirm('${msg.get("sub.confirmDelete")}')">
                    @if(seasonId != null)
                      <input type="hidden" name="seasonId" value="${seasonId}">
                    @endif
                    <button type="submit" class="btn btn-sm btn-outline-danger">${msg.get("action.delete")}</button>
                  </form>
                </div>
              </td>
            </tr>
          @else
            <tr><td colspan="8" class="text-center text-body-secondary">${msg.get("info.noMembershipOption")}</td></tr>
          @endfor
        </tbody>
      </table>
    `)
  `
)
