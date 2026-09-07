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
@param java.util.List<com.github.gcolin.membership.MembershipOption> options
@param com.github.gcolin.membership.MembershipOption selectedOption
@param java.util.List<com.github.gcolin.membership.MembershipOptionFile> files
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
@param String error
@param Integer sent
!{i18ntitle = msg.get("page.membershipOptionFiles");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.membership.MembershipOption> safeOptions = options != null ? options : java.util.List.of();}
!{java.util.List<com.github.gcolin.membership.MembershipOptionFile> safeFiles = files != null ? files : java.util.List.of();}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
!{Integer selectedOptionId = selectedOption != null ? selectedOption.getId() : null;}
!{String selectedOptionIdJs = selectedOptionId != null ? String.valueOf(selectedOptionId) : "null";}
!{String seasonQuery = seasonId != null ? "&seasonId=" + seasonId : "";}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.membershipOptionFiles"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.membershipOptionFiles"), helpText=msg.get("help.membershipOptionFiles"), icon="bi-folder")

    @if(error != null && !error.isBlank())
      <div class="alert alert-danger" role="alert">${msg.get("membership.files.error." + error)}</div>
    @endif
    @if(sent != null)
      <div class="alert alert-success" role="alert">${msg.get("mail.sent", sent)}</div>
    @endif

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <form method="get" action="${contextPath}/membership-option-files" class="d-flex flex-wrap align-items-end gap-3 mb-0 w-100">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="membership_files_season", autoSubmit=true, inline=true)
          <div>
            <label for="optionId" class="form-label mb-1">${msg.get("membership.files.option")}</label>
            @if(safeOptions.isEmpty())
              <select id="optionId" name="optionId" class="form-select" disabled>
                <option value="">${msg.get("membership.files.noOptionsAvailable")}</option>
              </select>
            @else
              <select id="optionId" name="optionId" class="form-select" onchange="this.form.submit()">
                @for(var opt : safeOptions)
                  @if(selectedOptionId != null && selectedOptionId.equals(opt.getId()))
                    <option value="${opt.getId()}" selected>${opt.getOptionValue() != null ? opt.getOptionValue() : String.valueOf(opt.getId())}</option>
                  @else
                    <option value="${opt.getId()}">${opt.getOptionValue() != null ? opt.getOptionValue() : String.valueOf(opt.getId())}</option>
                  @endif
                @endfor
              </select>
            @endif
          </div>
        </form>
      `)

      @if(selectedOption == null || selectedOptionId == null)
        <p class="text-muted mb-0 mt-3">${msg.get("membership.files.noOptionsAvailable")}</p>
      @else
        <div class="border rounded p-3 mb-4 mt-3">
          <h2 class="h6 mb-3">${msg.get("membership.files.uploadTitle")}</h2>
          <div class="row g-3 align-items-end">
            <div class="col-12 col-md-8">
              <label for="upload_file" class="form-label">${msg.get("membership.files.file")}</label>
              <input id="upload_file" type="file" class="form-control" required>
            </div>
            <div class="col-12 col-md-4">
              <button type="button" class="btn btn-primary w-100" id="upload_submit">${msg.get("action.upload")}</button>
            </div>
          </div>
          <div class="form-text mt-2">${msg.get("membership.files.uploadHint")}</div>
        </div>

        @if(safeFiles.isEmpty())
          <p class="text-muted mb-0">${msg.get("membership.files.empty")}</p>
        @else
          <table class="table table-sm table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>${msg.get("membership.files.name")}</th>
                <th class="d-none d-md-table-cell">${msg.get("membership.files.size")}</th>
                <th class="d-none d-md-table-cell">${msg.get("membership.files.uploadedBy")}</th>
                <th>${msg.get("label.action")}</th>
              </tr>
            </thead>
            <tbody>
              @for(var file : safeFiles)
                <tr>
                  <td>
                    <a href="${contextPath}/membership-option-files/${file.getId()}/download">${file.getOriginalName()}</a>
                  </td>
                  <td class="d-none d-md-table-cell">
                    @if(file.getSizeBytes() != null)
                      ${String.format(java.util.Locale.FRANCE, "%.1f", file.getSizeBytes() / 1024.0)} Ko
                    @endif
                  </td>
                  <td class="d-none d-md-table-cell">${file.getUploadedBy() != null ? file.getUploadedBy() : ""}</td>
                  <td>
                    <div class="d-flex gap-2">
                      <a class="btn btn-sm btn-outline-primary" href="${contextPath}/membership-option-files/${file.getId()}/mail${seasonId != null ? "?seasonId=" + seasonId : ""}">${msg.get("action.sendMail")}</a>
                      <form method="post" class="m-0" action="${contextPath}/membership-option-files/${file.getId()}/delete" onsubmit="return confirm('${msg.get("sub.confirmDelete")}')">
                        <input type="hidden" name="optionId" value="${selectedOptionId}">
                        @if(seasonId != null)
                          <input type="hidden" name="seasonId" value="${seasonId}">
                        @endif
                        <button type="submit" class="btn btn-sm btn-outline-danger">${msg.get("action.delete")}</button>
                      </form>
                    </div>
                  </td>
                </tr>
              @endfor
            </tbody>
          </table>
        @endif
      @endif
    `)
  `,
  scripts = @`
    <script>
      (function () {
        var submit = document.getElementById('upload_submit');
        var fileInput = document.getElementById('upload_file');
        var optionId = ${selectedOptionIdJs};
        if (!submit || !fileInput || optionId == null) {
          return;
        }
        submit.addEventListener('click', async function () {
          var file = fileInput.files && fileInput.files[0];
          if (!file) {
            alert('${msg.get("membership.files.error.invalidFile")}');
            return;
          }
          submit.disabled = true;
          try {
            var url = '${contextPath}/membership-option-files/upload'
              + '?optionId=' + encodeURIComponent(optionId)
              + '&filename=' + encodeURIComponent(file.name)
              + '${seasonQuery}';
            var res = await fetch(url, {
              method: 'POST',
              headers: { 'Content-Type': file.type || 'application/octet-stream' },
              body: file,
              redirect: 'follow'
            });
            if (res.redirected) {
              window.location.href = res.url;
              return;
            }
            if (!res.ok) {
              alert('${msg.get("membership.files.error.uploadFailed")}');
              submit.disabled = false;
              return;
            }
            window.location.reload();
          } catch (e) {
            alert('${msg.get("membership.files.error.uploadFailed")}');
            submit.disabled = false;
          }
        });
      })();
    </script>
  `
)
