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
@param java.util.List<com.github.gcolin.registration.PlayerSubscriptionOption> options
@param com.github.gcolin.player.IPlayer player
@param com.github.gcolin.registration.PlayerSubscription sub
!{i18ntitle = msg.get("action.edit");}
!{title = i18ntitle;}
!{page = "sub";}
!{if (options == null) options = java.util.List.of();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
    <div class="bg-body p-3 rounded mb-3">
    <div class="row justify-content-center mt-3">
        <form id="editForm" method="post" action="${contextPath}/event/${sub.getEvent().getId()}/register/${sub.getId()}">

            <input type="hidden" name="toRemove" id="toRemove" value="false">

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("sub.nrFFE")}
                </label>
                <input type="text" id="nrFFE" name="nrFFE" value="${sub.getNrFfe() != null ? sub.getNrFfe() : ""}" class="form-control" required>
                @if(player != null)
                <div class="form-text">
                    ${player.getFirstname() != null ? player.getFirstname() : ""} ${player.getName() != null ? player.getName() : ""}
                </div>
                @endif
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("sub.eventId")}
                </label>
                <input type="text" id="eventId" name="eventId" value="${sub.getEvent().getId()}" class="form-control" required>
                @if(sub.getEvent() != null)
                <div class="form-text">
                    ${sub.getEvent().getName() != null ? sub.getEvent().getName() : ""}
                    <a href="${contextPath}/event/${sub.getEvent().getId()}">${msg.get("action.view")}</a>
                </div>
                @endif
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("sub.creationUser")}
                </label>
                <input type="text" id="creationUser" name="creationUser" value="${sub.getCreationUser() != null ? sub.getCreationUser() : ""}" class="form-control" required>
            </div>

            <div class="mb-3">
                <label class="form-label">${msg.get("sub.createdAt")}</label>
                <div class="form-control-plaintext">
                    @if(sub.getCreatedAt() != null)
                        ${sub.getCreatedAt().toString()}
                    @else
                        —
                    @endif
                </div>
            </div>

            <div class="mb-3">
                <label class="form-label">${msg.get("sub.updatedAt")}</label>
                <div class="form-control-plaintext">
                    @if(sub.getUpdatedAt() != null)
                        ${sub.getUpdatedAt().toString()}
                    @else
                        —
                    @endif
                </div>
            </div>

            <div class="mb-3">
                <label class="form-label">${msg.get("event.status")}</label>
                <select id="status" name="status" class="form-select" required>
                    @if(sub.getStatus() != null && sub.getStatus() == com.github.gcolin.registration.PlayerSubscriptionStatus.NOT_PAID)
                    <option value="NOT_PAID" selected>${msg.get("playerStatus.NOT_PAID")}</option>
                    @else
                    <option value="NOT_PAID">${msg.get("playerStatus.NOT_PAID")}</option>
                    @endif
                    @if(sub.getStatus() != null && sub.getStatus() == com.github.gcolin.registration.PlayerSubscriptionStatus.PAID)
                    <option value="PAID" selected>${msg.get("playerStatus.PAID")}</option>
                    @else
                    <option value="PAID">${msg.get("playerStatus.PAID")}</option>
                    @endif
                    @if(sub.getStatus() != null && sub.getStatus() == com.github.gcolin.registration.PlayerSubscriptionStatus.CANCELLED)
                    <option value="CANCELLED" selected>${msg.get("playerStatus.CANCELLED")}</option>
                    @else
                    <option value="CANCELLED">${msg.get("playerStatus.CANCELLED")}</option>
                    @endif
                </select>
            </div>

            <div class="mb-3">
                <label class="form-label">${msg.get("player.attendance")}</label>
                <div id="attendance-at" class="form-control-plaintext">
                    @if(sub.getAttendanceAt() != null)
                        ${sub.getAttendanceAt().toString()}
                    @else
                        ${msg.get("player.attendance.none")}
                    @endif
                </div>
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("sub.amountCents")}
                </label>
                <input type="number" id="amountCents" name="amountCents" value="${sub.getAmountCents() != null ? sub.getAmountCents() : 0}" class="form-control" min="0" step="1" required>
            </div>

            @if(sub.getPayment() != null && sub.getPayment().getId() != null)
            <div class="mb-3">
                <div>
                    <a class="btn btn-secondary" href="${contextPath}/payment/${sub.getPayment().getId()}/edit">
                        ${msg.get("action.goToPayment")}
                    </a>
                </div>
            </div>
            @endif

            <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <label class="form-label mb-0">${msg.get("sub.option.list")}</label>
                    <a class="btn btn-sm btn-outline-primary"
                       href="${contextPath}/event/${sub.getEvent().getId()}/register/${sub.getId()}/option/new">
                        ${msg.get("sub.option.create")}
                    </a>
                </div>
                @if(options != null && options.size() > 0)
                <div class="table-responsive">
                    <table class="table table-sm align-middle mb-0">
                        <thead>
                            <tr>
                                <th>${msg.get("sub.option.description")}</th>
                                <th>${msg.get("sub.amountCents")}</th>
                                <th>${msg.get("event.status")}</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            @for(var pso : options)
                            <tr>
                                <td>${pso.getDescription() != null ? pso.getDescription() : ""}</td>
                                <td>${String.format(java.util.Locale.FRANCE, "%.2f", (pso.getAmountCents() != null ? pso.getAmountCents() : 0) / 100.0)} €</td>
                                <td>${pso.getStatus() != null ? msg.get("playerStatus." + pso.getStatus().name()) : ""}</td>
                                <td class="text-end">
                                    <a href="${contextPath}/event/${sub.getEvent().getId()}/register/${sub.getId()}/option/${pso.getId()}">
                                        ${msg.get("action.edit")}
                                    </a>
                                </td>
                            </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
                @else
                <div class="form-control-plaintext text-body-secondary">${msg.get("sub.option.empty")}</div>
                @endif
            </div>

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
