@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.payment.Payment payment
<div class="dropdown">
  <button class="btn btn-sm btn-link text-body-secondary text-decoration-none border-0 py-0 px-1" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="${msg.get("label.action")}" title="${msg.get("label.action")}">
    <i class="bi bi-three-dots-vertical"></i>
  </button>
  <ul class="dropdown-menu dropdown-menu-end">
    <li><a class="dropdown-item" href="${contextPath}/payment/${payment.getId()}/edit">${msg.get("action.edit")}</a></li>
  </ul>
</div>
