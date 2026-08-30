@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.event.Event event
@param boolean showView
@param boolean showUploadPapi
@if(user.canViewEventConfig() || showView || user.hasRole("EVENT_ADMIN") || user.hasRole("ADMIN"))
<div class="dropdown">
  <button class="btn btn-sm btn-link text-body-secondary text-decoration-none border-0 py-0 px-1" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="${msg.get("label.action")}" title="${msg.get("label.action")}">
    <i class="bi bi-three-dots-vertical"></i>
  </button>
  <ul class="dropdown-menu dropdown-menu-end">
    @if(user.canViewEventConfig())
    <li><a class="dropdown-item" id="edit" href="${contextPath}/event/${event.getId()}/edit">@if(user.canManageEvents())${msg.get("action.edit")}@else${msg.get("action.viewConfig")}@endif</a></li>
    @endif
    @if(showView)
    <li><a class="dropdown-item" href="${contextPath}/event/${event.getId()}">${msg.get("action.view")}</a></li>
    @endif
    @if(user.hasRole("ADMIN"))
    <li><a class="dropdown-item" id="statistics" href="${contextPath}/event/${event.getId()}/statistics">${msg.get("event.statistics")}</a></li>
    @endif
    @if(user.canViewEventConfig())
    <li><a class="dropdown-item" id="desk" href="${contextPath}/event/${event.getId()}/desk">${msg.get("event.desk.title")}</a></li>
    @endif
    @if(user.canViewEventConfig())
    <li><a class="dropdown-item" id="exportPapi" href="${contextPath}/event/${event.getId()}/papi">${msg.get("action.exportPapi")}</a></li>
    <li><a class="dropdown-item" id="exportCsv" href="${contextPath}/event/${event.getId()}/csv">${msg.get("action.exportCsv")}</a></li>
    <li><a class="dropdown-item" id="licenceCheck" href="${contextPath}/event/${event.getId()}/licencecheck">${msg.get("action.licenceCheck")}</a></li>
    @endif
    @if(user.canManageEvents())
    <li><a class="dropdown-item" id="sendMail" href="${contextPath}/event/${event.getId()}/mail">${msg.get("mail.sendToAll")}</a></li>
    @endif
    @if(showUploadPapi && user.canManageEvents())
    <li><button type="button" class="dropdown-item" id="uploadPapi">${msg.get("action.uploadPapi")}</button></li>
    @endif
  </ul>
</div>
@endif
