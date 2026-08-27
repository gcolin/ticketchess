@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventGroup eventGroup
@param com.github.gcolin.notification.Notification notif
@param java.util.List<com.github.gcolin.notification.Notification> notifs
@param com.github.gcolin.registration.PlayerSubscription sub
    !{i18ntitle = msg.get("page.notifications");}
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
            <li class="breadcrumb-item active" aria-current="page">${msg.get("page.notifications")}</li>
          </ol>
        </nav>
        <div>
      <a class="btn btn-dark mb-3" href="${contextPath}/notification/new">${msg.get("action.createGeneric")}</a>
		</div>
            <table class="table table-striped">
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">${msg.get("sub.eventId")}</th>
                    <th scope="col">${msg.get("notification.eventGroupId")}</th>
                    <th scope="col">${msg.get("mail.body")}</th>
                  </tr>
                </thead>
                <tbody>

                  @for(var n : notifs)
                  <tr>
                    <td><a href="${contextPath}/notification/${n.getId()}">${n.getId()}</a></td>
                    <td>@if(n.getEvent() != null)${n.getEvent().getId()}@endif</td>
                    <td>@if(n.getEventGroup() != null)${n.getEventGroup().getId()}@endif</td>
                    <td>${n.getContent()}</td>
                  </tr>
                  @endfor
                </tbody>
              </table>
        </main>
		@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    </body>

    </html>