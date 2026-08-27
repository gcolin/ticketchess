@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param com.github.gcolin.auth.LoggedUser user
@param String[] templates
!{String title = msg.get("page.adminMail");}
!{String page = "adminmail";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("page.adminMail")}</li>
      </ol>
    </nav>
    <h1>Send Email</h1>
    <form method="post" action="${contextPath}/admin/mail" class="mt-3">
      <div class="mb-3">
        <label for="to" class="form-label">Recipient email</label>
        <input type="email" class="form-control" id="to" name="to" required>
      </div>
      <div class="mb-3">
        <label for="subject" class="form-label">Subject</label>
        <input type="text" class="form-control" id="subject" name="subject" required>
      </div>
      <div class="mb-3">
        <label for="template" class="form-label">Template</label>
        <select class="form-select" id="template" name="template" required>
          @for(var tmpl : templates)
            <option value="${tmpl}">${tmpl}</option>
          @endfor
        </select>
      </div>
      <div class="mb-3">
        <label for="name" class="form-label">Name</label>
        <input type="text" class="form-control" id="name" name="name">
      </div>
      <div class="mb-3">
        <label for="eventName" class="form-label">Event Name</label>
        <input type="text" class="form-control" id="eventName" name="eventName">
      </div>
      <div class="mb-3">
        <label for="evenDate" class="form-label">Event Date</label>
        <input type="text" class="form-control" id="evenDate" name="evenDate">
      </div>
      <div class="mb-3">
        <label for="amount" class="form-label">Amount</label>
        <input type="text" class="form-control" id="amount" name="amount">
      </div>
      <div class="mb-3">
        <label for="reference" class="form-label">Reference</label>
        <input type="text" class="form-control" id="reference" name="reference">
      </div>
      <div class="mb-3">
        <label for="baseUrl" class="form-label">Base URL (optional)</label>
        <input type="text" class="form-control" id="baseUrl" name="baseUrl">
      </div>
      <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary">Send</button>
        <button
          type="submit"
          class="btn btn-outline-secondary"
          formaction="${contextPath}/admin/mail/preview"
          formtarget="mailPreviewFrame"
          formnovalidate
        >
          Preview
        </button>
      </div>
    </form>

    <div class="mt-4">
      <h2 class="h5">Preview</h2>
      <iframe
        name="mailPreviewFrame"
        title="Email preview"
        class="w-100 border rounded"
        style="min-height: 500px; background-color: #f4f6f8;"
      ></iframe>
    </div>
  </main>
  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>
