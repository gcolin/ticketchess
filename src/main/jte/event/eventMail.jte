@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.event.Event event
@param java.util.List<com.github.gcolin.event.Event> events
@param com.github.gcolin.membership.MembershipOption option
@param Object p
@param Object recipient
@param java.util.List<com.github.gcolin.event.EventMailApi.MailRecipient> recipients
@param Object sent
@param String status
@param Object subject
@param Object type
!{title = event.getName();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "events";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <div class="bg-body p-3 rounded mb-3">
        <h2>${event.getName()} &mdash; ${msg.get("mail.sendToAll")}</h2>

        @if(sent != null)
          <div class="alert alert-success" role="alert">
            ${msg.get("mail.sent", sent)}
          </div>
        @endif

        <form method="post" action="${contextPath}/event/${event.getId()}/mail" accept-charset="UTF-8">

          <div class="mb-3">
            <label for="subject" class="form-label">${msg.get("mail.subject")}</label>
            <input type="text" id="subject" name="subject" class="form-control" required>
          </div>

          <div class="mb-3">
            <label for="body" class="form-label">${msg.get("mail.body")}</label>
            <textarea id="body" name="body" class="form-control" rows="15" required></textarea>
          </div>

          <div class="mb-3">
            <label class="form-label">${msg.get("mail.preview")}</label>
            <iframe id="mail-preview" class="border rounded w-100" style="min-height:400px;background:#f4f6f8;" frameborder="0"></iframe>
          </div>

          <div class="mb-3">
            <label for="subscriptionStatus" class="form-label">${msg.get("mail.subscriptionStatus")}</label>
            <select id="subscriptionStatus" name="subscriptionStatus" class="form-control">
              <option value="" selected>${msg.get("mail.subscriptionStatus.all")}</option>
              <option value="NOT_PAID">${msg.get("playerStatus.NOT_PAID")}</option>
              <option value="PAID">${msg.get("playerStatus.PAID")}</option>
              <option value="CANCELLED">${msg.get("playerStatus.CANCELLED")}</option>
            </select>
          </div>

          <div class="mb-3">
            <div class="form-check mb-2">
              <input type="checkbox" class="form-check-input" id="recipientFilterEnabled" name="recipientFilterEnabled" value="true">
              <label class="form-check-label" for="recipientFilterEnabled">${msg.get("mail.recipients.enable")}</label>
            </div>

            <div id="recipientFilterPanel" class="d-none">
              <div class="d-flex justify-content-between align-items-center">
                <label class="form-label mb-0">${msg.get("mail.recipients")}</label>
                <button type="button" id="toggleRecipients" class="btn btn-sm btn-outline-secondary">${msg.get("mail.recipients.unselectAll")}</button>
              </div>
              <div id="recipient-list" class="border rounded p-2 mt-2" style="max-height:220px;overflow:auto;">
                @if(recipients != null && recipients.size() > 0)
                    @for(var rec : recipients)
                    <div class="form-check mb-1">
                      <input class="form-check-input recipient-checkbox" type="checkbox" name="selectedSubscriptions" value="${rec.getId()}" id="recipient-${rec.getId()}" data-status="${rec.getStatus().name()}">
                      <label class="form-check-label" for="recipient-${rec.getId()}">
                        ${rec.getLastname()} ${rec.getFirstname()} - ${rec.getEmail()} (${msg.get("playerStatus." + rec.getStatus().name())})
                      </label>
                    </div>
                  @endfor
                @else
                  <div class="text-body-secondary small">${msg.get("mail.recipients.none")}</div>
                @endif
              </div>
            </div>
          </div>

          <div class="mb-3 form-check">
            <input type="checkbox" class="form-check-input" id="sendAsBcc" name="sendAsBcc" value="true">
            <label class="form-check-label" for="sendAsBcc">${msg.get("mail.sendAsBcc")}</label>
          </div>

          <div class="mb-3 form-check">
            <input type="checkbox" class="form-check-input" id="sendInBatches" name="sendInBatches" value="true">
            <label class="form-check-label" for="sendInBatches">${msg.get("mail.sendInBatches")}</label>
            <div class="form-text">${msg.get("mail.sendInBatches.help")}</div>
          </div>

          <div class="d-flex gap-2">
            <a class="btn btn-secondary" href="${contextPath}/event/${event.getId()}">${msg.get("action.cancel")}</a>
            <button type="submit" class="btn btn-primary">&#9993; ${msg.get("mail.send")}</button>
          </div>
        </form>
      </div>
    </main>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
<script>
(function () {
  var bodyEl = document.getElementById('body');
  var previewFrame = document.getElementById('mail-preview');
  var subscriptionStatusEl = document.getElementById('subscriptionStatus');
  var recipientFilterEnabledEl = document.getElementById('recipientFilterEnabled');
  var recipientFilterPanelEl = document.getElementById('recipientFilterPanel');
  var sendAsBcc = document.getElementById('sendAsBcc');
  var sendInBatches = document.getElementById('sendInBatches');
  var recipientCheckboxes = document.querySelectorAll('.recipient-checkbox');
  var toggleRecipientsButton = document.getElementById('toggleRecipients');
  var timer = null;

  function updatePreview() {
    var formData = new FormData();
    formData.append('body', bodyEl.value || '');
    formData.append('bcc', (sendAsBcc.checked || (sendInBatches && sendInBatches.checked)) ? "true" : "false");
    if (recipientFilterEnabledEl && recipientFilterEnabledEl.checked) {
      formData.append('recipientFilterEnabled', 'true');
      recipientCheckboxes.forEach(function (checkbox) {
        if (checkbox.checked) {
          formData.append('selectedSubscriptions', checkbox.value);
        }
      });
    }

    fetch('${contextPath}/event/${event.getId()}/mail/preview', {
      method: 'POST',
      body: new URLSearchParams(formData)
    })
    .then(function(r) { return r.text(); })
    .then(function(html) {
      var doc = previewFrame.contentDocument || previewFrame.contentWindow.document;
      doc.open();
      doc.write(html);
      doc.close();
    });
  }

  function scheduleUpdate() {
    clearTimeout(timer);
    timer = setTimeout(updatePreview, 400);
  }

  function updateRecipientPanelVisibility() {
    if (!recipientFilterEnabledEl || !recipientFilterPanelEl) {
      return;
    }
    if (recipientFilterEnabledEl.checked) {
      recipientFilterPanelEl.classList.remove('d-none');
    } else {
      recipientFilterPanelEl.classList.add('d-none');
    }
  }

  function syncSelectionWithStatus() {
    var selectedStatus = subscriptionStatusEl ? subscriptionStatusEl.value : '';
    recipientCheckboxes.forEach(function (checkbox) {
      checkbox.checked = !selectedStatus || checkbox.dataset.status === selectedStatus;
    });
  }

  bodyEl.addEventListener('input', scheduleUpdate);
  if (subscriptionStatusEl) {
    subscriptionStatusEl.addEventListener('change', function () {
      if (recipientFilterEnabledEl && recipientFilterEnabledEl.checked) {
        syncSelectionWithStatus();
      }
      refreshToggleButtonLabel();
      updatePreview();
    });
  }
  if (recipientFilterEnabledEl) {
    recipientFilterEnabledEl.addEventListener('change', function () {
      updateRecipientPanelVisibility();
      if (recipientFilterEnabledEl.checked) {
        syncSelectionWithStatus();
      }
      refreshToggleButtonLabel();
      updatePreview();
    });
  }
  sendAsBcc.addEventListener('change', updatePreview);
  if (sendInBatches) {
    sendInBatches.addEventListener('change', function () {
      if (sendInBatches.checked) {
        sendAsBcc.checked = true;
      }
      updatePreview();
    });
  }
  recipientCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener('change', updatePreview);
  });

  function areAllRecipientsSelected() {
    if (recipientCheckboxes.length === 0) {
      return false;
    }
    return Array.prototype.every.call(recipientCheckboxes, function (checkbox) { return checkbox.checked; });
  }

  function refreshToggleButtonLabel() {
    if (!toggleRecipientsButton) {
      return;
    }
    toggleRecipientsButton.textContent = areAllRecipientsSelected()
      ? "${msg.get("mail.recipients.unselectAll")}"
      : "${msg.get("mail.recipients.selectAll")}";
  }

  if (toggleRecipientsButton) {
    toggleRecipientsButton.addEventListener('click', function () {
      var selectAll = !areAllRecipientsSelected();
      recipientCheckboxes.forEach(function (checkbox) {
        checkbox.checked = selectAll;
      });
      refreshToggleButtonLabel();
      updatePreview();
    });
  }

  recipientCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener('change', refreshToggleButtonLabel);
  });

  updateRecipientPanelVisibility();
  refreshToggleButtonLabel();
  updatePreview();
})();
</script>
</body>
</html>