@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.event.EventCollection eventCollection
@param java.util.List<com.github.gcolin.event.EventCollection> eventCollections
@param com.github.gcolin.membership.MembershipOption option
@param Object p
@param Object sent
@param Object subject
@param Object type
!{title = eventCollection.getName();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "admin";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3 mt-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/eventcollection">${msg.get("page.eventCollections")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">&#9993; ${msg.get("mail.sendToAll")}</li>
        </ol>
      </nav>

      <div class="bg-body p-3 rounded mb-3">
        <h2>${eventCollection.getName()} &mdash; ${msg.get("mail.sendToAll")}</h2>

        @if(sent != null)
          <div class="alert alert-success" role="alert">
            ${msg.get("mail.sent", sent)}
          </div>
        @endif

        <form method="post" action="${contextPath}/eventcollection/${eventCollection.getId()}/mail" accept-charset="UTF-8">

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

          <div class="row g-3 mb-3">
            <div class="col-sm-6">
              <label for="minElo" class="form-label">${msg.get("mail.minElo")}</label>
              <input type="number" id="minElo" name="minElo" class="form-control" min="0" placeholder="${msg.get("mail.elo.noLimit")}">
            </div>
            <div class="col-sm-6">
              <label for="maxElo" class="form-label">${msg.get("mail.maxElo")}</label>
              <input type="number" id="maxElo" name="maxElo" class="form-control" min="0" placeholder="${msg.get("mail.elo.noLimit")}">
            </div>
          </div>

          <div class="mb-3 form-check">
            <input type="checkbox" class="form-check-input" id="sendAsBcc" name="sendAsBcc" value="true">
            <label class="form-check-label" for="sendAsBcc">${msg.get("mail.sendAsBcc")}</label>
          </div>

          <div class="d-flex gap-2">
            <a class="btn btn-secondary" href="${contextPath}/eventcollection/${eventCollection.getId()}">${msg.get("action.cancel")}</a>
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
  var sendAsBcc = document.getElementById('sendAsBcc');
  var timer = null;

  function updatePreview() {
    var formData = new FormData();
    formData.append('body', bodyEl.value || '');
    formData.append('bcc', sendAsBcc.checked ? "true" : "false");

    fetch('${contextPath}/eventcollection/${eventCollection.getId()}/mail/preview', {
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

  bodyEl.addEventListener('input', scheduleUpdate);
  sendAsBcc.addEventListener('change', updatePreview);
  updatePreview();
})();
</script>
</body>
</html>