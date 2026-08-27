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
@param Object p
@param com.github.gcolin.registration.PlayerSubscription sub
@param Object type
!{i18ntitle = msg.get("action.edit");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "sub";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3 mt-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
            <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
            <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
            <li class="breadcrumb-item"><a href="${contextPath}/notification">${msg.get("page.notifications")}</a></li>
            <li class="breadcrumb-item active" aria-current="page">${msg.get("action.edit")}</li>
        </ol>
    </nav>
    <div class="bg-body p-3 rounded mb-3">
    <div class="row justify-content-center">
        <form id="editForm" method="post" action="${contextPath}/notification" accept-charset="UTF-8">

            <!-- Event ID (hidden for edit) -->
            <input type="hidden" name="id" value="${notif.getId()}">

            <input type="hidden" name="toRemove" id="toRemove" value="false">

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("sub.eventId")}
                </label>
                <input type="text" id="eventId" name="eventId" value="@if(notif.getEvent() != null)${notif.getEvent().getId()}@endif" class="form-control">
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("label.eventGroup")}
                </label>
                <input type="text" id="eventGroupId" name="eventGroupId" value="@if(notif.getEventGroup() != null)${notif.getEventGroup().getId()}@endif" class="form-control">
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("event.description")}
                </label>
                <textarea id="content" name="content" class="form-control" rows="20">${notif.getContent()}</textarea>
            </div>

            <!-- Submit -->
            <div class="d-flex justify-content-between">
                <button type="button"
                        class="btn btn-outline-danger"
                        onclick="removeAndSubmit()">
                    🗑️ ${msg.get("action.remove")}
                </button>

                <button type="submit" class="btn btn-outline-primary">
                    💾 ${msg.get("action.save")}
                </button>
            </div>

        </form>


<script>
    function removeAndSubmit() {
        if (confirm('${msg.get("sub.confirmDelete")}')) {
            document.getElementById('toRemove').value = 'true';
            document.getElementById('editForm').submit();
        }
    }
</script>
    </div></div>
    </main>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>