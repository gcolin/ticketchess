@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.event.EventGroup group
<div class="d-flex align-items-center gap-2">
  <span>${group.getName()}</span>
  <div class="dropdown">
    <button class="btn btn-sm btn-link text-body-secondary text-decoration-none border-0 py-0 px-1" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="${msg.get("label.action")}" title="${msg.get("label.action")}">
      <i class="bi bi-three-dots-vertical"></i>
    </button>
    <ul class="dropdown-menu">
      <li><a class="dropdown-item" href="${contextPath}/eventgroup/${group.getId()}">${msg.get("action.edit")}</a></li>
      <li><a class="dropdown-item" href="${contextPath}/event/${group.getShortname()}">${msg.get("action.view")}</a></li>
      @if(user.hasPermission("ADMIN_PANEL"))
      <li><a class="dropdown-item" href="${contextPath}/statistics">${msg.get("event.statistics")}</a></li>
      @endif
    </ul>
  </div>
</div>
