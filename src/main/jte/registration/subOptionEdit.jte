@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param String currentStatus
@param com.github.gcolin.event.Event event
@param boolean isNew
@param com.github.gcolin.registration.PlayerSubscriptionOption option
@param com.github.gcolin.registration.PlayerSubscription sub
!{i18ntitle = isNew ? msg.get("sub.option.create") : msg.get("action.edit");}
!{title = i18ntitle;}
!{page = "sub";}
!{if (option == null) option = new com.github.gcolin.registration.PlayerSubscriptionOption();}
!{if (currentStatus == null && option.getStatus() != null) currentStatus = option.getStatus().name();}
!{if (currentStatus == null) currentStatus = "NOT_PAID";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
    <div class="bg-body p-3 rounded mb-3">
    <div class="row justify-content-center mt-3">
        <form id="editForm" method="post" action="${isNew ? contextPath + "/event/" + sub.getEvent().getId() + "/register/" + sub.getId() + "/option" : contextPath + "/event/" + sub.getEvent().getId() + "/register/" + sub.getId() + "/option/" + option.getId()}">
            @if(!isNew)
            <input type="hidden" name="toRemove" id="toRemove" value="false">
            @endif

            <div class="mb-3">
                <label class="form-label">${msg.get("sub.option.parent")}</label>
                <div class="form-control-plaintext">
                    #${sub.getId()}
                    <a href="${contextPath}/event/${sub.getEvent().getId()}/register/${sub.getId()}">${msg.get("action.view")}</a>
                </div>
            </div>

            <div class="mb-3">
                <label class="form-label" for="description">${msg.get("sub.option.description")}</label>
                <input type="text" id="description" name="description"
                       value="${option.getDescription() != null ? option.getDescription() : ""}" class="form-control" required maxlength="512">
            </div>

            <div class="mb-3">
                <label class="form-label" for="amountCents">${msg.get("sub.amountCents")}</label>
                <input type="number" id="amountCents" name="amountCents"
                       value="${option.getAmountCents() != null ? option.getAmountCents() : 0}" class="form-control" min="0" step="1" required>
            </div>

            <div class="mb-3">
                <label class="form-label" for="status">${msg.get("event.status")}</label>
                <select id="status" name="status" class="form-select" required>
                    @if("NOT_PAID".equals(currentStatus))
                    <option value="NOT_PAID" selected>${msg.get("playerStatus.NOT_PAID")}</option>
                    @else
                    <option value="NOT_PAID">${msg.get("playerStatus.NOT_PAID")}</option>
                    @endif
                    @if("PAID".equals(currentStatus))
                    <option value="PAID" selected>${msg.get("playerStatus.PAID")}</option>
                    @else
                    <option value="PAID">${msg.get("playerStatus.PAID")}</option>
                    @endif
                </select>
            </div>

            @if(option != null && option.getPayment() != null && option.getPayment().getId() != null)
            <div class="mb-3">
                <a class="btn btn-secondary" href="${contextPath}/payment/${option.getPayment().getId()}/edit">
                    ${msg.get("action.goToPayment")}
                </a>
            </div>
            @endif

            <div class="d-flex justify-content-between">
                @if(!isNew)
                <button type="button"
                        class="btn btn-outline-danger"
                        onclick="removeAndSubmit()">
                    🗑️ ${msg.get("action.remove")}
                </button>
                @else
                <a class="btn  btn-secondary" href="${contextPath}/event/${sub.getEvent().getId()}/register/${sub.getId()}">
                    ${msg.get("action.cancel")}
                </a>
                @endif

                <button type="submit" class="btn btn-outline-primary">
                    💾 ${msg.get("action.save")}
                </button>
            </div>

        </form>

@if(!isNew)
<script>
    function removeAndSubmit() {
        if (confirm('${msg.get("sub.option.confirmDelete")}')) {
            document.getElementById('toRemove').value = 'true';
            document.getElementById('editForm').submit();
        }
    }
</script>
@endif
    </div></div>
    </main>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>
