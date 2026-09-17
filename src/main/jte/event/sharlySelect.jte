@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param String callback
@param java.util.List<com.github.gcolin.event.SharlySelectItem> items
@param String error
!{i18ntitle = msg.get("sharlySelect.title");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "sharlySelect";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
  <main class="container flex-fill">
    <div class="bg-body p-3 rounded mb-3">
      <h2>${msg.get("sharlySelect.heading")}</h2>
      <p>${msg.get("sharlySelect.description")}</p>
    </div>

    @if(error != null)
    <div class="alert alert-danger" role="alert">
      @if("invalidCallback".equals(error))
        ${msg.get("sharlySelect.error.invalidCallback")}
      @elseif("notFound".equals(error))
        ${msg.get("sharlySelect.error.notFound")}
      @else
        ${error}
      @endif
    </div>
    @endif

    @if(error == null || !"invalidCallback".equals(error))
    <div class="bg-body p-3 rounded mb-3">
      @if(items == null || items.isEmpty())
        <p class="text-body-secondary mb-0">${msg.get("sharlySelect.empty")}</p>
      @else
        <div class="list-group">
          @for(var item : items)
            <form method="post" action="${contextPath}/sharly/select" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
              <input type="hidden" name="callback" value="${callback}">
              <input type="hidden" name="kind" value="${item.getKind().name()}">
              @if(item.getCollectionId() != null)
              <input type="hidden" name="collectionId" value="${item.getCollectionId()}">
              @endif
              @if(item.getEventId() != null)
              <input type="hidden" name="eventId" value="${item.getEventId()}">
              @endif
              <div>
                <div class="fw-semibold">${item.getName()}</div>
                <div class="small text-body-secondary">
                  @if(item.isCollection())
                    ${msg.get("sharlySelect.collection")} · ${item.getTournamentCount()} ${msg.get("sharlySelect.tournaments")}
                  @else
                    ${msg.get("sharlySelect.event")}
                  @endif
                  · ${item.getChessEventId()}
                </div>
              </div>
              <button type="submit" class="btn btn-primary btn-sm">${msg.get("sharlySelect.import")}</button>
            </form>
          @endfor
        </div>
      @endif
    </div>
    @endif
  </main>
  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>
