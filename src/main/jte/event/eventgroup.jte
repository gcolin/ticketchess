@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.event.EventGroup eventGroup
@param java.util.List<com.github.gcolin.event.EventGroup> eventGroups
@param java.util.List<com.github.gcolin.notification.Notification> notifs
@param com.github.gcolin.player.DisplayPlayer player
    !{i18ntitle = msg.get("page.eventGroups");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

    <html>

    <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "notifs";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
		
		
        <main class="container flex-fill">
        <nav aria-label="breadcrumb" class="mb-3 mt-3">
          <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
            <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
            <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
            <li class="breadcrumb-item active" aria-current="page">${msg.get("page.eventGroups")}</li>
          </ol>
        </nav>
        <div>
  <a class="btn btn-dark mb-3" href="${contextPath}/eventgroup/new">${msg.get("action.createGeneric")}</a>
	</div>
            <table class="table table-striped">
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">${msg.get("player.name")}</th>
                    <th scope="col">${msg.get("label.shortname")}</th>
                  </tr>
                </thead>
                <tbody>

                  @if(eventGroups != null)
                  @for(var eg : eventGroups)
                  <tr>
                    <td><a href="${contextPath}/eventgroup/${eg.getId()}">${eg.getId()}</a></td>
                    <td>${eg.getName()}</td>
                    <td>${eg.getShortname()}</td>
                  </tr>
                  @endfor
                  @endif
                </tbody>
              </table>
        </main>
		@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    </body>

    </html>