@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.Map<String, Boolean> canSelectEvent
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventCollection eventCollection
@param java.util.List<com.github.gcolin.event.Event> events
@param java.util.Map<String, String> ineligibleReason
@param Integer currentEventId
@param com.github.gcolin.registration.PlayerSubscription playerSubscription
!{i18ntitle = msg.get("selectEventCollectionVariant.title");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "selectEventCollectionVariant";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
  <main class="container flex-fill">
    <div class="bg-body p-3 rounded mb-3">
      <h2>${msg.get("selectEventCollectionVariant.heading")}</h2>
      <p class="mb-1"><strong>${eventCollection.getName()}</strong></p>
      <p>${msg.get("selectEventCollectionVariant.description")}</p>
    </div>

    <div class="bg-body p-3 rounded mb-3">
      <form method="post" action="${contextPath}/event/my/${playerSubscription.getId()}/eventcollection/${eventCollection.getId()}">
        <div class="mb-3">
          <label class="form-label">${msg.get("selectEventCollectionVariant.selectEvent")}</label>
          <div>
            @for(var ev : events)
              @if(java.util.Objects.nonNull(canSelectEvent.get(String.valueOf(ev.getId()))) && Boolean.FALSE.equals(canSelectEvent.get(String.valueOf(ev.getId()))))
                @if(ev.getId().equals(currentEventId))
                <div class="form-check mb-2">
                  <input class="form-check-input" type="radio" name="eventId" id="event_${ev.getId()}" value="${ev.getId()}" checked disabled>
                  <label class="form-check-label" for="event_${ev.getId()}">${ev.getName()}</label>
                </div>
                @else
                <div class="form-check mb-2">
                  <input class="form-check-input" type="radio" name="eventId" id="event_${ev.getId()}" value="${ev.getId()}" disabled>
                  <label class="form-check-label" for="event_${ev.getId()}">${ev.getName()}</label>
                </div>
                @endif
              @else
                @if(ev.getId().equals(currentEventId))
                <div class="form-check mb-2">
                  <input class="form-check-input" type="radio" name="eventId" id="event_${ev.getId()}" value="${ev.getId()}" checked>
                  <label class="form-check-label" for="event_${ev.getId()}">${ev.getName()}</label>
                </div>
                @else
                <div class="form-check mb-2">
                  <input class="form-check-input" type="radio" name="eventId" id="event_${ev.getId()}" value="${ev.getId()}">
                  <label class="form-check-label" for="event_${ev.getId()}">${ev.getName()}</label>
                </div>
                @endif
              @endif
            @endfor
          </div>
        </div>

        <div class="d-flex gap-2">
          <button type="submit" class="btn btn-primary">${msg.get("action.change")}</button>
          <a href="${contextPath}/event/my" class="btn btn-secondary">${msg.get("action.cancel")}</a>
        </div>
      </form>
    </div>
  </main>
  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>