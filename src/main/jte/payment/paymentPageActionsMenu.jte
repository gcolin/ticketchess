@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param Integer seasonId
<div class="dropdown">
  <button class="btn btn-sm btn-link text-body-secondary text-decoration-none border-0 py-0 px-1" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="${msg.get("label.action")}" title="${msg.get("label.action")}">
    <i class="bi bi-three-dots-vertical"></i>
  </button>
  <ul class="dropdown-menu dropdown-menu-end">
    <li><a class="dropdown-item" href="${contextPath}/payment/export/pdf${seasonId != null ? "?seasonId=" + seasonId : ""}"><i class="bi bi-file-earmark-pdf me-2"></i>${msg.get("action.exportAccountingPdf")}</a></li>
    <li><hr class="dropdown-divider"></li>
    <li><a class="dropdown-item" href="${contextPath}/payment/export/csv${seasonId != null ? "?seasonId=" + seasonId : ""}">${msg.get("action.exportCsv")}</a></li>
    <li><a class="dropdown-item" href="${contextPath}/payment/export${seasonId != null ? "?seasonId=" + seasonId : ""}" target="_blank" rel="noopener noreferrer">${msg.get("action.exportJson")}</a></li>
    <li><a class="dropdown-item" href="${contextPath}/payment/audit${seasonId != null ? "?seasonId=" + seasonId : ""}">Audit montants</a></li>
  </ul>
</div>
