@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<com.github.gcolin.club.ClubSeason> seasons
!{i18ntitle = msg.get("page.seasons");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.club.ClubSeason> safeSeasons = seasons != null ? seasons : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("breadcrumb.seasons"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.seasons"), helpText=msg.get("help.seasons"), icon="bi-calendar-range")

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <a class="btn btn-success" href="${contextPath}/admin/seasons/new">${msg.get("action.addSeason")}</a>
      `)
      <table class="table table-striped align-middle mb-0 mt-3">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("label.seasonName")}</th>
            <th scope="col">${msg.get("label.startDate")}</th>
            <th scope="col">${msg.get("label.endDate")}</th>
            <th scope="col">${msg.get("label.seasonCurrent")}</th>
            <th scope="col">${msg.get("label.action")}</th>
          </tr>
        </thead>
        <tbody>
          @for(var season : safeSeasons)
            <tr>
              <td>${season.getId()}</td>
              <td>${season.getName() != null ? season.getName() : ""}</td>
              <td>@if(season.getStartDate() != null)${season.getStartDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}@endif</td>
              <td>@if(season.getEndDate() != null)${season.getEndDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}@endif</td>
              <td>
                @if(season.isCurrent())
                  <span class="badge bg-primary">${msg.get("label.yes")}</span>
                @else
                  <span class="text-body-secondary">—</span>
                @endif
              </td>
              <td>
                <div class="d-flex gap-2">
                  <a class="btn btn-sm btn-outline-primary" href="${contextPath}/admin/seasons/${season.getId()}/edit">${msg.get("action.edit")}</a>
                  <form method="post" action="${contextPath}/admin/seasons/${season.getId()}/delete" class="m-0" onsubmit="return confirm('${msg.get("action.confirmDeleteSeason")}')">
                    <button type="submit" class="btn btn-sm btn-outline-danger">${msg.get("action.delete")}</button>
                  </form>
                </div>
              </td>
            </tr>
          @else
            <tr><td colspan="6" class="text-center text-body-secondary">${msg.get("seasons.empty")}</td></tr>
          @endfor
        </tbody>
      </table>
    `)
  `
)
