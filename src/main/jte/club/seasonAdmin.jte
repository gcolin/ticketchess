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
!{page = "admin";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3 mt-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("breadcrumb.seasons")}</li>
      </ol>
    </nav>

    <div class="card mb-4">
      <div class="card-body">
        <h1 class="mb-2">${msg.get("page.seasons")}</h1>
        <p class="text-body-secondary mb-0">${msg.get("help.seasons")}</p>
      </div>
    </div>

    <div class="mb-3">
      <a class="btn btn-success" href="${contextPath}/admin/seasons/new">${msg.get("action.addSeason")}</a>
    </div>

    <div class="table-responsive bg-body rounded p-2">
      <table class="table table-striped align-middle mb-0">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("label.seasonName")}</th>
            <th scope="col">${msg.get("label.startDate")}</th>
            <th scope="col">${msg.get("label.endDate")}</th>
            <th scope="col">${msg.get("label.seasonCurrent")}</th>
            <th scope="col">${msg.get("label.actions")}</th>
          </tr>
        </thead>
        <tbody>
          @for(var season : seasons)
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
    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>
