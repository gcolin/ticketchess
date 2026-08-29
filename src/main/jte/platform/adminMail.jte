@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param com.github.gcolin.auth.LoggedUser user
@param String[] templates
!{String title = msg.get("page.adminMail");}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.adminMail"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.adminMail"), helpText=msg.get("admin.tile.mailDesc"), icon="bi-envelope")

    <div class="row g-4">
      <div class="col-lg-6">
        @template.platform.adminPanel(
          heading=msg.get("mail.send"),
          content = @`
            <form method="post" action="${contextPath}/admin/mail">
              <div class="row g-3">
                <div class="col-12">
                  <label for="to" class="form-label">${msg.get("mail.recipientEmail")}</label>
                  <input type="email" class="form-control" id="to" name="to" required>
                </div>
                <div class="col-12">
                  <label for="subject" class="form-label">${msg.get("mail.subject")}</label>
                  <input type="text" class="form-control" id="subject" name="subject" required>
                </div>
                <div class="col-12">
                  <label for="template" class="form-label">${msg.get("mail.template")}</label>
                  <select class="form-select" id="template" name="template" required>
                    @for(var tmpl : templates)
                      <option value="${tmpl}">${tmpl}</option>
                    @endfor
                  </select>
                </div>
                <div class="col-sm-6">
                  <label for="name" class="form-label">${msg.get("mail.name")}</label>
                  <input type="text" class="form-control" id="name" name="name">
                </div>
                <div class="col-sm-6">
                  <label for="eventName" class="form-label">${msg.get("mail.eventName")}</label>
                  <input type="text" class="form-control" id="eventName" name="eventName">
                </div>
                <div class="col-sm-6">
                  <label for="evenDate" class="form-label">${msg.get("mail.eventDate")}</label>
                  <input type="text" class="form-control" id="evenDate" name="evenDate">
                </div>
                <div class="col-sm-6">
                  <label for="amount" class="form-label">${msg.get("mail.amount")}</label>
                  <input type="text" class="form-control" id="amount" name="amount">
                </div>
                <div class="col-sm-6">
                  <label for="reference" class="form-label">${msg.get("mail.reference")}</label>
                  <input type="text" class="form-control" id="reference" name="reference">
                </div>
                <div class="col-sm-6">
                  <label for="baseUrl" class="form-label">${msg.get("mail.baseUrl")}</label>
                  <input type="text" class="form-control" id="baseUrl" name="baseUrl">
                </div>
              </div>
              <div class="d-flex gap-2 mt-4">
                <button type="submit" class="btn btn-primary">
                  <i class="bi bi-send me-1"></i>${msg.get("mail.send")}
                </button>
                <button
                  type="submit"
                  class="btn  btn-secondary"
                  formaction="${contextPath}/admin/mail/preview"
                  formtarget="mailPreviewFrame"
                  formnovalidate
                >
                  <i class="bi bi-eye me-1"></i>${msg.get("mail.preview")}
                </button>
              </div>
            </form>
          `
        )
      </div>

      <div class="col-lg-6">
        @template.platform.adminPanel(
          heading=msg.get("mail.preview"),
          content = @`
            <iframe
              name="mailPreviewFrame"
              title="${msg.get("mail.preview")}"
              class="w-100 border-0 rounded-bottom"
              style="min-height: 560px; background-color: #f4f6f8;"
            ></iframe>
          `
        )
      </div>
    </div>
  `
)
