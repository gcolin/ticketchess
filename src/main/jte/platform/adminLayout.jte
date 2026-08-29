@import gg.jte.Content
@param String title
@param String contextPath
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param com.github.gcolin.platform.StateService state
@param com.github.gcolin.auth.LoggedUser user
@param Content content
@param Content alerts = null
@param Content scripts = null
@param Content breadcrumbTrail = null
@param String activePageTitle = null
@param Boolean adminHub = false
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page="admin", msg=msg)
    <main class="container flex-fill">
      @if(adminHub)
        @template.platform.adminBreadcrumb(contextPath=contextPath, msg=msg, activePageTitle=msg.get("menu.admin"), adminHub=true)
      @elseif(activePageTitle != null)
        @template.platform.adminBreadcrumb(contextPath=contextPath, msg=msg, activePageTitle=activePageTitle, trail=breadcrumbTrail)
      @endif
      @if(alerts != null)
        ${alerts}
      @endif
      ${content}
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    @if(scripts != null)
      ${scripts}
    @endif
  </body>
</html>
