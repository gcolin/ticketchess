@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.membership.Membership membership
@param Integer seasonId
<div class="dropdown">
  <button class="btn btn-sm btn-link text-body-secondary text-decoration-none border-0 py-0 px-1" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="${msg.get("label.action")}" title="${msg.get("label.action")}">
    <i class="bi bi-three-dots-vertical"></i>
  </button>
  <ul class="dropdown-menu dropdown-menu-end">
    <li><a class="dropdown-item" href="${contextPath}/membership/${membership.getId()}/edit">${msg.get("action.edit")}</a></li>
    <li><hr class="dropdown-divider"></li>
    <li>
      <form method="post" class="m-0" action="${contextPath}/membership/${membership.getId()}/delete" onsubmit="return confirm('${msg.get("sub.confirmDelete")}')">
        @if(seasonId != null)
          <input type="hidden" name="seasonId" value="${seasonId}">
        @endif
        <button type="submit" class="dropdown-item text-danger">${msg.get("action.delete")}</button>
      </form>
    </li>
  </ul>
</div>
