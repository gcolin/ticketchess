@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param String filterUrl
@param java.util.List<com.github.gcolin.platform.SelectItem> eventgroups
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventCollection eventCollection
@param com.github.gcolin.event.EventGroup eventGroup
@param Object eventgroup
@param java.util.List<com.github.gcolin.event.Event> events
@param Object filter
@param com.github.gcolin.membership.MembershipOption option
@param Object p
@param String status
@param Object type
!{i18ntitle = msg.get("events.title");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

    <main class="container flex-fill">
        <div class="bg-body p-3 rounded mb-3">
        <h1>${msg.get("home.title")}</h1> <p class="lead">${msg.get("home.description")}</p>
        </div>
    
        <div class="d-flex justify-content-end">
            <button id="filterbutton" class="btn btn-sm btn-dark mb-2"
                    type="button"
                    data-bs-toggle="collapse"
                    data-bs-target="#filters"
                    aria-expanded="true">
                <i class="bi bi-filter"></i> ${msg.get("label.filters")}
            </button>
        </div>
        <div class="collapse" id="filters">
            <div class="row">
                <div class="col">
                    <div class="btn-group mb-3" role="group">
                        @if(user.canManageEvents())
                        @if("DRAFT".equals(status))
                        <a id="event_draft" href="${contextPath}${filterUrl}?status=DRAFT" class="btn btn-info active">${msg.get("status.DRAFT")}</a>
                        @else
                        <a id="event_draft" href="${contextPath}${filterUrl}?status=DRAFT" class="btn btn-info">${msg.get("status.DRAFT")}</a>
                        @endif
                        @endif
                        @if("ACTIVE".equals(status))
                        <a id="event_active" href="${contextPath}${filterUrl}?status=ACTIVE" class="btn btn-info active">${msg.get("status.ACTIVE")}</a>
                        @else
                        <a id="event_active" href="${contextPath}${filterUrl}?status=ACTIVE" class="btn btn-info">${msg.get("status.ACTIVE")}</a>
                        @endif
                        @if("ONGOING".equals(status))
                        <a id="event_ongoing" href="${contextPath}${filterUrl}?status=ONGOING" class="btn btn-info active">${msg.get("status.ONGOING")}</a>
                        @else
                        <a id="event_ongoing" href="${contextPath}${filterUrl}?status=ONGOING" class="btn btn-info">${msg.get("status.ONGOING")}</a>
                        @endif
                        @if("COMPLETED".equals(status))
                        <a id="event_completed" href="${contextPath}${filterUrl}?status=COMPLETED" class="btn btn-info active">${msg.get("status.COMPLETED")}</a>
                        @else
                        <a id="event_completed" href="${contextPath}${filterUrl}?status=COMPLETED" class="btn btn-info">${msg.get("status.COMPLETED")}</a>
                        @endif
                    </div>
                </div>
                <div class="col">
                    <div class="input-group mb-3">
                        <label class="input-group-text" for="inputCategory">${msg.get("label.category")}</label>
                        <select class="form-select" id="inputCategory">
                            @if(eventgroups != null)
                            @for(var egFilter : eventgroups)
                            @if(egFilter.isSelected())
                            <option value="${egFilter.getValue()}" selected>${egFilter.getLabel()}</option>
                            @else
                            <option value="${egFilter.getValue()}">${egFilter.getLabel()}</option>
                            @endif
                            @endfor
                            @endif
                        </select>

                        <script type="text/javascript">
                        document.getElementById("inputCategory").addEventListener("change", e => {
                            var newLocation = "${contextPath}/event";
                            if (e.target.value) {
                                newLocation += "/" + e.target.value;
                            }
                            window.location = newLocation;
                        });
                        </script>
                    </div>
                </div>
            </div>
        </div>
        @if(eventGroup != null)
        <div class="bg-body p-3 mb-3">
            <h2>${eventGroup.getName()}</h2>
        </div>
        @endif
        @if(events.size() == 0)
        <div id="noevent" class="bg-body p-3 pb-1 rounded mb-3">
            <p>${msg.get("event.noevent")}</p>
        </div>
        @else
        <div class="rounded" style="overflow: hidden;"> 
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th scope="col">${msg.get("event.name")}</th>
                <th scope="col"><i class="bi bi-calendar3 me-2"></i> ${msg.get("event.start")}</th>
                <th scope="col"><i class="bi bi-clock me-2"></i> ${msg.get("event.type")}</th>
           </tr>
           </thead>
           <tbody>
                @for(var ev : events)
		            <tr>
		                <td>
		                    <a href="${contextPath}/event/${ev.getId()}">${ev.getName()}</a>
                            @if((ev.getMaxSubscriptions() != null && ev.getMaxSubscriptions() == ev.getNbSubscriptions())
                                 || (ev.getEventCollection() != null
                                     && ev.getEventCollection().getMaxSubscribe() != null
                                     && ev.getEventCollection().getNbSubscriptions() != null
                                     && ev.getEventCollection().getMaxSubscribe() == ev.getEventCollection().getNbSubscriptions()))
		                    <span class="badge bg-info">${msg.get("event.full2")}</span>
		                    @endif
		                </td>
		                <td>
		                    ${ev.getStartDateAsDate() != null ? ev.getStartDateAsDate().toString() : ""}
		                </td>
		                <td>${ev.getEventType() != null ? msg.get("type." + ev.getEventType().name()) : ""}
		                </td>
		            </tr>
              @endfor
           </tbody>
        </table>
		</div>
        @endif
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>