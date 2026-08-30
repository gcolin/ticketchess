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
@param boolean createMode
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventCollection eventCollection
@param java.util.List<com.github.gcolin.event.EventCollection> eventCollections
@param java.util.List<com.github.gcolin.event.Event> events
@param Object message
@param Object p
@param String status
@param Object type
@param String chessEventId
!{i18ntitle = createMode ? msg.get("action.createGeneric") : msg.get("action.edit");}
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
          <li class="breadcrumb-item"><a href="${contextPath}/admin/events">${msg.get("breadcrumb.events")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">@if(createMode)${msg.get("eventcollection.createBreadcrumb")}@else${msg.get("eventcollection.editBreadcrumb")}@endif</li>
        </ol>
      </nav>

      <div class="card mb-3">
        <div class="card-body">
          <h1 class="h4 mb-2">${msg.get("heading.eventCollections")}</h1>
          <p class="text-body-secondary mb-0">${msg.get("help.eventCollections")}</p>
        </div>
      </div>

      @if(success != null && "save".equals(success))
        <div class="alert alert-success" role="alert">${msg.get("message.saveSuccess")}</div>
      @endif
      @if(error != null)
        <div class="alert alert-danger" role="alert">${msg.get("eventcollection.error." + error)}</div>
      @endif

      <div class="card mb-3">
        <div class="card-body">
          <form method="post" action="${contextPath}/eventcollection" class="row g-2 align-items-end">
            <input type="hidden" name="action" value="@if(createMode)create@elseupdate@endif">
            @if(!createMode)
              <input type="hidden" name="id" value="${eventCollection.getId()}">
            @endif
            <div class="col-12 col-md-8 col-lg-6">
              <label class="form-label" for="eventcollection_name">${msg.get("event.name")}</label>
              <input id="eventcollection_name" type="text" name="name" value="${(eventCollection.getName()) != null ? (eventCollection.getName()) : ""}" class="form-control" required>
            </div>
            <div class="col-12 col-md-4 col-lg-3">
              <label class="form-label" for="eventcollection_maxsubscribe">${msg.get("eventcollection.maxSubscribe")}</label>
              <input id="eventcollection_maxsubscribe" type="text" inputmode="numeric" pattern="[0-9]*" name="maxSubscribe" value="@if(eventCollection.getMaxSubscribe() != null)${String.valueOf(eventCollection.getMaxSubscribe())}@endif" placeholder="${msg.get("eventcollection.maxSubscribe.placeholder")}" class="form-control">
            </div>
            <div class="col-12 col-md-6 col-lg-4">
              <label class="form-label" for="eventcollection_chesseventid">${msg.get("eventcollection.chessEventId")}</label>
              <input id="eventcollection_chesseventid" type="text" name="chessEventId" value="${chessEventId != null ? chessEventId : ""}" class="form-control" placeholder="${msg.get("eventcollection.chessEventId.placeholder")}">
              <div class="form-text">${msg.get("eventcollection.chessEventId.help")}</div>
            </div>
            <div class="col-12 col-md-auto">
              <button type="submit" class="btn btn-primary">@if(createMode)${msg.get("action.createGeneric")}@else${msg.get("action.save")}@endif</button>
            </div>
          </form>
          @if(!createMode)
          <p class="text-body-secondary mt-3 mb-0">
            @if(eventCollection.getMaxSubscribe() != null)
              ${msg.get("eventcollection.currentSubscriptionsWithMax", eventCollection.getNbSubscriptions() != null ? eventCollection.getNbSubscriptions() : 0, eventCollection.getMaxSubscribe())}
            @else
              ${msg.get("eventcollection.currentSubscriptions", eventCollection.getNbSubscriptions() != null ? eventCollection.getNbSubscriptions() : 0)}
            @endif
          </p>
          @endif
          @if(!createMode)
          <form method="post" action="${contextPath}/eventcollection" class="row g-2 mt-1">
            <input type="hidden" name="action" value="remove">
            <input type="hidden" name="id" value="${eventCollection.getId()}">
            <div class="col-12 col-md-auto">
              @if(Integer.compare(eventCollection.getEventCount(), 0) == 1)
              <button type="submit" class="btn btn-outline-danger" disabled>${msg.get("action.remove")}</button>
              @else
              <button type="submit" class="btn btn-outline-danger">${msg.get("action.remove")}</button>
              @endif
            </div>
          </form>
          @endif
        </div>
      </div>

      @if(!createMode && eventCollection.getEvents() != null && eventCollection.getEvents().size() > 0)
      <div class="card mb-3">
        <div class="card-body">
          <h2 class="h5 mb-3">${msg.get("page.events")}</h2>
          <div class="table-responsive">
            <table class="table table-sm table-hover mb-0">
              <thead class="table-light">
                <tr>
                  <th>${msg.get("event.name")}</th>
                  <th>${msg.get("event.start")}</th>
                  <th>${msg.get("event.status")}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for(var ev : eventCollection.getEvents())
                <tr>
                  <td>${ev.getName()}</td>
                  <td>@if(ev.getStartDateAsDate() != null)${ev.getStartDateAsDate().toString()}@endif</td>
                  <td>
                    @if(ev.getStatus() != null && "DRAFT".equals(ev.getStatus().name()))
                    <span class="badge bg-secondary">${msg.get("event.status.DRAFT")}</span>
                    @elseif(ev.getStatus() != null && "ACTIVE".equals(ev.getStatus().name()))
                    <span class="badge bg-success">${msg.get("event.status.ACTIVE")}</span>
                    @elseif(ev.getStatus() != null && "ONGOING".equals(ev.getStatus().name()))
                    <span class="badge bg-warning">${msg.get("event.status.ONGOING")}</span>
                    @elseif(ev.getStatus() != null && "COMPLETED".equals(ev.getStatus().name()))
                    <span class="badge bg-danger">${msg.get("event.status.COMPLETED")}</span>
                    @elseif(ev.getStatus() != null)
                    <span class="badge bg-info">${msg.get("event.status." + ev.getStatus().name())}</span>
                    @endif
                  </td>
                  <td class="text-end">
                    <a href="${contextPath}/event/${ev.getId()}" class="btn btn-sm btn-outline-primary">${msg.get("action.edit")}</a>
                  </td>
                </tr>
                @endfor
              </tbody>
            </table>
          </div>
        </div>
      </div>
      @endif
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
  </body>
</html>