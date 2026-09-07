@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.MembershipOptionFile file
@param com.github.gcolin.membership.MembershipOption option
@param java.util.List<com.github.gcolin.membership.MembershipOptionFileAdminApi.MailRecipient> recipients
@param String defaultBody
@param String defaultSubject
@param Integer seasonId
@param Integer sent
!{i18ntitle = msg.get("page.membershipOptionFileMail");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.membership.MembershipOptionFileAdminApi.MailRecipient> safeRecipients = recipients != null ? recipients : java.util.List.of();}
!{String seasonParam = seasonId != null ? "?seasonId=" + seasonId : "";}
!{String filesBackUrl = contextPath + "/membership-option-files?optionId=" + option.getId() + (seasonId != null ? "&seasonId=" + seasonId : "");}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.membershipOptionFileMail"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/membership-option-files${seasonParam}">${msg.get("page.membershipOptionFiles")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.membershipOptionFileMail"), helpText=msg.get("help.membershipOptionFileMail"), icon="bi-envelope")

    @if(sent != null)
      <div class="alert alert-success" role="alert">${msg.get("mail.sent", sent)}</div>
    @endif

    @template.platform.adminPanel(content = @`
      <p class="text-body-secondary mb-3">
        <span class="fw-semibold">${file.getOriginalName()}</span>
        <span class="text-muted">— ${option.getOptionValue() != null ? option.getOptionValue() : String.valueOf(option.getId())}</span>
      </p>

      <form method="post" action="${contextPath}/membership-option-files/${file.getId()}/mail" accept-charset="UTF-8">
        @if(seasonId != null)
          <input type="hidden" name="seasonId" value="${seasonId}">
        @endif

        <div class="mb-3">
          <label for="subject" class="form-label">${msg.get("mail.subject")}</label>
          <input type="text" id="subject" name="subject" class="form-control" value="${defaultSubject != null ? defaultSubject : ""}" required>
        </div>

        <div class="mb-3">
          <label for="body" class="form-label">${msg.get("mail.body")} (Markdown)</label>
          <textarea id="body" name="body" class="form-control" rows="12" required>${defaultBody != null ? defaultBody : ""}</textarea>
        </div>

        <div class="mb-3">
          <label class="form-label">${msg.get("mail.preview")}</label>
          <iframe id="mail-preview" class="border rounded w-100" style="min-height:400px;background:#f4f6f8;" frameborder="0" title="${msg.get("mail.preview")}"></iframe>
        </div>

        <div class="mb-3">
          <label class="form-label">${msg.get("mail.recipients")} (${safeRecipients.size()})</label>
          <div class="border rounded p-2" style="max-height:220px;overflow:auto;">
            @if(safeRecipients.isEmpty())
              <div class="text-body-secondary small">${msg.get("mail.recipients.none")}</div>
            @else
              <ul class="list-unstyled mb-0">
                @for(var recipient : safeRecipients)
                  <li class="mb-1">
                    @if(recipient.displayName() != null && !recipient.displayName().isBlank())
                      ${recipient.displayName()} —
                    @endif
                    ${recipient.email()}
                  </li>
                @endfor
              </ul>
            @endif
          </div>
        </div>

        <div class="d-flex gap-2 justify-content-end">
          <a class="btn btn-secondary" href="${filesBackUrl}">${msg.get("action.cancel")}</a>
          @if(safeRecipients.isEmpty())
            <button type="submit" class="btn btn-primary" disabled>&#9993; ${msg.get("mail.send")}</button>
          @else
            <button type="submit" class="btn btn-primary">&#9993; ${msg.get("mail.send")}</button>
          @endif
        </div>
      </form>
    `)
  `,
  scripts = @`
    <script>
      (function () {
        var bodyEl = document.getElementById('body');
        var previewFrame = document.getElementById('mail-preview');
        if (!bodyEl || !previewFrame) {
          return;
        }
        var timer = null;
        function updatePreview() {
          var formData = new URLSearchParams();
          formData.append('body', bodyEl.value || '');
          fetch('${contextPath}/membership-option-files/${file.getId()}/mail/preview', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: formData
          })
          .then(function (r) { return r.text(); })
          .then(function (html) {
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
        updatePreview();
      })();
    </script>
  `
)
