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
@param Object p
    !{i18ntitle = msg.get("home.title");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

    <html>

    <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "home";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
        <main class="container flex-fill"> <div class="bg-body p-5 rounded">
        <h1>${msg.get("home.title")}</h1> <p class="lead">${msg.get("home.description")}</p>
        <a class="btn btn-lg btn-primary" href="${contextPath}/event" role="button">${msg.get("home.button")} »</a>
        </div> </main>

		@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    </body>

    </html>