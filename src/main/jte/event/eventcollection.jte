@param String contextPath
@param String error
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param boolean created
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventCollection eventCollection
@param java.util.List<com.github.gcolin.event.EventCollection> eventCollections
@param java.util.List<com.github.gcolin.event.Event> events
@param Object message
@param Object p
@param boolean removed
@param Object statistics
@param boolean updated
!{i18ntitle = msg.get("page.eventCollections");}
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
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.eventCollections")}</li>
        </ol>
      </nav>

      <div class="card mb-3">
        <div class="card-body">
          <h1 class="h4 mb-2">${msg.get("heading.eventCollections")}</h1>
          <p class="text-body-secondary mb-0">${msg.get("help.eventCollections")}</p>
        </div>
      </div>

      @if(created)
        <div class="alert alert-success" role="alert">${msg.get("message.saveSuccess")}</div>
      @endif
      @if(updated)
        <div class="alert alert-success" role="alert">${msg.get("message.saveSuccess")}</div>
      @endif
      @if(removed)
        <div class="alert alert-success" role="alert">${msg.get("message.removeSuccess")}</div>
      @endif
      @if(error != null)
        <div class="alert alert-danger" role="alert">${msg.get("eventcollection.error." + error)}</div>
      @endif

      <div class="card mb-3">
        <div class="card-body">
          <a href="${contextPath}/eventcollection/new" class="btn btn-primary">${msg.get("action.createGeneric")}</a>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-body p-0">
          <table class="table table-striped table-hover mb-0 align-middle">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">${msg.get("event.name")}</th>
                <th scope="col">${msg.get("label.events")}</th>
                <th scope="col">${msg.get("eventcollection.maxSubscribe")}</th>
                <th scope="col">${msg.get("label.action")}</th>
              </tr>
            </thead>
            <tbody>
              @for(var ec : eventCollections)
              <tr>
                <td>${ec.getId()}</td>
                <td>
                  <a href="${contextPath}/eventcollection/${ec.getId()}">${ec.getName()}</a>
                </td>
                <td>${ec.getEventCount()}</td>
                <td>
                  @if(ec.getMaxSubscribe() != null)
                    ${(ec.getNbSubscriptions() != null ? ec.getNbSubscriptions() : 0)} / ${ec.getMaxSubscribe()}
                  @else
                    -
                  @endif
                </td>
                <td class="d-flex gap-1">
                  <a href="${contextPath}/eventcollection/${ec.getId()}" class="btn btn-sm btn-outline-primary">${msg.get("action.edit")}</a>
                  <a href="${contextPath}/eventcollection/${ec.getId()}/statistics" class="btn btn-sm btn-outline-info">&#128202; ${msg.get("event.statistics")}</a>
                  <a href="${contextPath}/eventcollection/${ec.getId()}/mail" class="btn btn-sm btn-outline-secondary">&#9993; ${msg.get("mail.send")}</a>
                </td>
              </tr>
              @endfor
              @if(!((eventCollections != null && !eventCollections.isEmpty())))
              <tr>
                <td colspan="5" class="text-center text-body-secondary p-3">${msg.get("eventcollection.empty")}</td>
              </tr>
              @endif
            </tbody>
          </table>
        </div>
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
  </body>
</html>