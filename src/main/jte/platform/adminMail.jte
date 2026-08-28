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
<body class="d-flex flex-column min-vh-100 bg-body-secondary">
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
  <main class="container flex-fill py-3">
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("page.adminMail")}</li>
      </ol>
    </nav>

    <div class="card mb-4">
      <div class="card-body">
        <div class="d-flex align-items-start gap-3">
          <i class="bi bi-envelope fs-2 text-primary"></i>
          <div>
            <h1 class="h3 mb-1">${msg.get("page.adminMail")}</h1>
            <p class="text-body-secondary mb-0">${msg.get("admin.tile.mailDesc")}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="row g-4">
      <div class="col-lg-6">
        <div class="card h-100">
          <div class="card-header">
            <h2 class="h5 mb-0">${msg.get("mail.send")}</h2>
          </div>
          <div class="card-body">
            <form method="post" action="${contextPath}/admin/mail">
              <div class="row g-3">
                <div class="col-12">
                  <label for="to" class="form-label">Recipient email</label>
                  <input type="email" class="form-control" id="to" name="to" required>
                </div>
                <div class="col-12">
                  <label for="subject" class="form-label">${msg.get("mail.subject")}</label>
                  <input type="text" class="form-control" id="subject" name="subject" required>
                </div>
                <div class="col-12">
                  <label for="template" class="form-label">Template</label>
                  <select class="form-select" id="template" name="template" required>
                    @for(var tmpl : templates)
                      <option value="${tmpl}">${tmpl}</option>
                    @endfor
                  </select>
                </div>
                <div class="col-sm-6">
                  <label for="name" class="form-label">Name</label>
                  <input type="text" class="form-control" id="name" name="name">
                </div>
                <div class="col-sm-6">
                  <label for="eventName" class="form-label">Event Name</label>
                  <input type="text" class="form-control" id="eventName" name="eventName">
                </div>
                <div class="col-sm-6">
                  <label for="evenDate" class="form-label">Event Date</label>
                  <input type="text" class="form-control" id="evenDate" name="evenDate">
                </div>
                <div class="col-sm-6">
                  <label for="amount" class="form-label">Amount</label>
                  <input type="text" class="form-control" id="amount" name="amount">
                </div>
                <div class="col-sm-6">
                  <label for="reference" class="form-label">Reference</label>
                  <input type="text" class="form-control" id="reference" name="reference">
                </div>
                <div class="col-sm-6">
                  <label for="baseUrl" class="form-label">Base URL (optional)</label>
                  <input type="text" class="form-control" id="baseUrl" name="baseUrl">
                </div>
              </div>
              <div class="d-flex gap-2 mt-4">
                <button type="submit" class="btn btn-primary">
                  <i class="bi bi-send me-1"></i>${msg.get("mail.send")}
                </button>
                <button
                  type="submit"
                  class="btn btn-outline-secondary"
                  formaction="${contextPath}/admin/mail/preview"
                  formtarget="mailPreviewFrame"
                  formnovalidate
                >
                  <i class="bi bi-eye me-1"></i>${msg.get("mail.preview")}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card h-100">
          <div class="card-header">
            <h2 class="h5 mb-0">${msg.get("mail.preview")}</h2>
          </div>
          <div class="card-body p-0">
            <iframe
              name="mailPreviewFrame"
              title="${msg.get("mail.preview")}"
              class="w-100 border-0 rounded-bottom"
              style="min-height: 560px; background-color: #f4f6f8;"
            ></iframe>
          </div>
        </div>
      </div>
    </div>
  </main>
  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>
