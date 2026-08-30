@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param Integer seasonId
<div class="dropdown">
  <button class="btn btn-sm btn-link text-body-secondary text-decoration-none border-0 py-0 px-1" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="${msg.get("label.action")}" title="${msg.get("label.action")}">
    <i class="bi bi-three-dots-vertical"></i>
  </button>
  <ul class="dropdown-menu dropdown-menu-end">
    <li><a class="dropdown-item" href="${contextPath}/membership/export/pdf${seasonId != null ? "?seasonId=" + seasonId : ""}"><i class="bi bi-file-earmark-pdf me-2"></i>${msg.get("action.exportMembershipsPdf")}</a></li>
    <li><hr class="dropdown-divider"></li>
    <li><a class="dropdown-item" href="${contextPath}/membership/export/csv${seasonId != null ? "?seasonId=" + seasonId : ""}">${msg.get("action.exportCsv")}</a></li>
  </ul>
</div>
