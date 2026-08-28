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
@param java.util.List<com.github.gcolin.event.EventGroup> eventGroups
@param java.util.List<com.github.gcolin.event.Event> events
@param java.util.List<com.github.gcolin.notification.Notification> egnotifications
@param Object p
@param Object type
!{i18ntitle = msg.get("action.edit");}
!{title = i18ntitle;}
!{if (eventGroup == null) eventGroup = new com.github.gcolin.event.EventGroup();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "event";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <div class="container flex-fill">
        <nav aria-label="breadcrumb" class="mb-3 mt-3">
            <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
                <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
                <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
                <li class="breadcrumb-item"><a href="${contextPath}/admin/events">${msg.get("breadcrumb.events")}</a></li>
                <li class="breadcrumb-item active" aria-current="page">@if(eventGroup.getId() != null)${msg.get("eventgroup.editBreadcrumb")}@else${msg.get("eventgroup.createBreadcrumb")}@endif</li>
            </ol>
        </nav>
        <div class="bg-body p-3 rounded mb-3">
        <ul class="nav nav-tabs" id="myTab" role="tablist">
            <li class="nav-item" role="presentation">
                <button class="nav-link active" id="home-tab" data-bs-toggle="tab"
                    data-bs-target="#general" type="button" role="tab" aria-controls="general"
                    aria-selected="true">
                    ${msg.get("event.generalinfo")}
                </button>
            </li>
            @if(eventGroup.getId() != null)
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="profile-tab" data-bs-toggle="tab"
                    data-bs-target="#egevents" type="button" role="tab" aria-controls="events"
                    aria-selected="false">
                    ${msg.get("label.events")}
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="profile-tab" data-bs-toggle="tab"
                    data-bs-target="#notifications" type="button" role="tab" aria-controls="notifs"
                    aria-selected="false">
                    ${msg.get("label.notifications")}
                </button>
            </li>
            @endif
        </ul>
        <div class="tab-content" id="myTabContent">
            <div class="tab-pane fade show active" id="general" role="tabpanel"
                aria-labelledby="general-tab">
                <div class="row justify-content-center mt-3">
                    <form method="post" action="${contextPath}/eventgroup">
                        @if(eventGroup.getId() != null)
                        <input type="hidden" name="id" value="${eventGroup.getId()}">
                        @endif

                        <div class="mb-3">
                            <label class="form-label">
                                #
                            </label>
                            <input id="id" type="text" value="${eventGroup.getId() != null ? String.valueOf(eventGroup.getId()) : ""}" class="form-control" disabled>
                        </div>

                        <!-- Event Name -->
                        <div class="mb-3">
                            <label class="form-label">
                                ${msg.get("event.name")}
                            </label>
                            <input id="event_name" type="text" name="name" value="${eventGroup.getName() != null ? eventGroup.getName() : ""}" class="form-control" required>
                        </div>

                         <!-- Event Name -->
                        <div class="mb-3">
                            <label class="form-label">
                                ${msg.get("label.shortname")}
                            </label>
                            <input id="shortname" type="text" name="shortname" value="${eventGroup.getShortname() != null ? eventGroup.getShortname() : ""}" class="form-control" required>
                        </div>

                        <!-- Submit -->
                        <div class="d-flex justify-content-end mb-3">
                            <button id="event_submit" type="submit" class="btn btn-primary">
                                &#128190; ${msg.get("action.save")}
                            </button>
                        </div>

                    </form>
                    @if(eventGroup.getId() != null)
                    <form method="post" action="${contextPath}/eventgroup" class="d-flex justify-content-end mb-3">
                        <input type="hidden" name="action" value="remove">
                        <input type="hidden" name="id" value="${eventGroup.getId()}">
                        <button type="submit" class="btn btn-danger">
                            &#10006; ${msg.get("action.remove")}
                        </button>
                    </form>
                    @endif
                </div>
            </div>
            @if(eventGroup.getId() != null)
            <div class="tab-pane fade" id="egevents" role="tabpanel"
                aria-labelledby="description-tab">
                <div class="row justify-content-center mt-3 p-3">
                    <ul>
                    @if(events != null)
                    @for(var ev : events)
                        <li><a href="${contextPath}/event/${ev.getId()}">${ev.getId()} - ${ev.getName()}</a></li>
                    @endfor
                    @endif
                    </ul>
                </div>
            </div>
            <div class="tab-pane fade" id="notifications" role="tabpanel"
                aria-labelledby="description-tab">
                <div class="row justify-content-center mt-3 p-3">
                    <ul>
                    @if(egnotifications != null)
                    @for(var notif : egnotifications)
                        <li><a href="${contextPath}/notification/${notif.getId()}">${notif.getId()} - ${notif.getShortContent()}</a></li>
                    @endfor
                    @endif
                    </ul>
                </div>
            </div>
            @endif
         </div>
    </div>
</div>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>