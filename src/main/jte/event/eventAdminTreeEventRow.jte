@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.event.Event event
<div class="d-flex flex-wrap align-items-center gap-2">
  <span class="text-body-secondary text-nowrap">
    @if(event.getStartDate() != null)
      ${msg.formatDate(event.getStartDate())}
    @else
      —
    @endif
  </span>
  <i class="bi bi-calendar-event text-body-secondary"></i>
  <span>${event.getName()}</span>
  @template.event.eventActionsMenu(contextPath=contextPath, msg=msg, user=user, event=event, showView=true, showUploadPapi=false)
</div>
