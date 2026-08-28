@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.club.ClubSeason season
!{i18ntitle = season.getId() != null ? msg.get("page.seasonEdit") : msg.get("page.seasonNew");}
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
        <li class="breadcrumb-item"><a href="${contextPath}/admin/seasons">${msg.get("breadcrumb.seasons")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${i18ntitle}</li>
      </ol>
    </nav>

    <div class="bg-body rounded p-3">
      <form method="post" action="${season.getId() != null ? contextPath + "/admin/seasons/" + season.getId() : contextPath + "/admin/seasons"}">
        <div class="row g-3">
          <div class="col-12">
            <label for="season_name" class="form-label">${msg.get("label.seasonName")}</label>
            <input id="season_name" type="text" name="name" class="form-control" value="${season.getName() != null ? season.getName() : ""}" required maxlength="255">
          </div>

          <div class="col-12 col-md-6">
            <label for="season_start" class="form-label">${msg.get("label.startDate")}</label>
            <input id="season_start" type="date" name="startDate" class="form-control" value="@if(season.getStartDate() != null)${season.getStartDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}@endif" required>
          </div>

          <div class="col-12 col-md-6">
            <label for="season_end" class="form-label">${msg.get("label.endDate")}</label>
            <input id="season_end" type="date" name="endDate" class="form-control" value="@if(season.getEndDate() != null)${season.getEndDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}@endif" required>
          </div>

          <div class="col-12">
            <div class="form-check">
              @if(season.isCurrent())
                <input id="season_current" type="checkbox" name="current" value="true" class="form-check-input" checked>
              @else
                <input id="season_current" type="checkbox" name="current" value="true" class="form-check-input">
              @endif
              <label for="season_current" class="form-check-label">${msg.get("label.seasonCurrent")}</label>
            </div>
          </div>
        </div>

        <div class="d-flex justify-content-end gap-2 mt-4">
          <a class="btn btn-outline-secondary" href="${contextPath}/admin/seasons">${msg.get("action.cancel")}</a>
          <button type="submit" class="btn btn-primary">${season.getId() != null ? msg.get("action.save") : msg.get("action.createSeason")}</button>
        </div>
      </form>
    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>
