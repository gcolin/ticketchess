@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<java.util.Map<String, Object>> mismatches
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.paymentAudit");}
!{title = i18ntitle;}
!{java.util.List<java.util.Map<String, Object>> safeMismatches = mismatches != null ? mismatches : java.util.List.of();}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("breadcrumb.audit"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/payment">${msg.get("breadcrumb.payments")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.paymentAudit"), helpText=msg.get("paymentAudit.heading"), icon="bi-shield-check")

    @template.platform.adminPanel(content = @`
      @template.platform.adminToolbar(content = @`
        <form method="get" action="${contextPath}/payment/audit" class="d-flex align-items-center mb-0">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="audit_season", autoSubmit=true, inline=true)
        </form>
      `)
    `)

    @if(!safeMismatches.isEmpty())
      @template.platform.adminPanel(
        subheading=msg.get("paymentAudit.mismatchCount", safeMismatches.size()),
        content = @`
        <table class="table table-striped align-middle mb-0">
          <thead>
            <tr>
              <th scope="col">#</th>
              <th scope="col">${msg.get("payment.email")}</th>
              <th scope="col">${msg.get("paymentAudit.paymentAmount")}</th>
              <th scope="col">${msg.get("paymentAudit.expectedSum")}</th>
              <th scope="col">${msg.get("paymentAudit.currentSum")}</th>
              <th scope="col">${msg.get("paymentAudit.gap")}</th>
              <th scope="col">${msg.get("paymentAudit.subscriptions")}</th>
              <th scope="col">${msg.get("label.action")}</th>
            </tr>
          </thead>
          <tbody>
            @for(var mismatch : safeMismatches)
              <tr>
                <td>
                  @if(mismatch.get("payment") != null)
                    <a href="${contextPath}/payment/${((com.github.gcolin.payment.Payment) mismatch.get("payment")).getId()}/edit">${((com.github.gcolin.payment.Payment) mismatch.get("payment")).getId()}</a>
                  @endif
                </td>
                <td>
                  @if(mismatch.get("payment") != null && ((com.github.gcolin.payment.Payment) mismatch.get("payment")).getUserEmail() != null)
                    ${((com.github.gcolin.payment.Payment) mismatch.get("payment")).getUserEmail()}
                  @endif
                </td>
                <td>${String.format(java.util.Locale.US, "%.2f", ((Long) mismatch.get("paymentCents")) / 100.0)} €</td>
                <td>${String.format(java.util.Locale.US, "%.2f", ((Long) mismatch.get("expectedCents")) / 100.0)} €</td>
                <td>${String.format(java.util.Locale.US, "%.2f", ((Long) mismatch.get("sumCents")) / 100.0)} €</td>
                @if(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents")) > 0)
                <td class="text-danger">
                  ${String.format(java.util.Locale.US, "%.2f", Math.abs(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents"))) / 100.0)} €
                  ${msg.get("paymentAudit.paymentHigher")}
                </td>
                @elseif(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents")) < 0)
                <td class="text-warning">
                  ${String.format(java.util.Locale.US, "%.2f", Math.abs(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents"))) / 100.0)} €
                  ${msg.get("paymentAudit.paymentLower")}
                </td>
                @elseif(mismatch.get("hasMissingAmount") != null && (Boolean) mismatch.get("hasMissingAmount"))
                <td class="text-warning">
                  ${String.format(java.util.Locale.US, "%.2f", Math.abs(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents"))) / 100.0)} €
                  ${msg.get("paymentAudit.missingAmounts")}
                </td>
                @else
                <td class="text-muted">
                  ${String.format(java.util.Locale.US, "%.2f", Math.abs(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents"))) / 100.0)} €
                  ${msg.get("paymentAudit.consistent")}
                </td>
                @endif
                <td>
                  @if(mismatch.get("subs") != null && !((java.util.List<?>) mismatch.get("subs")).isEmpty())
                    <ul class="mb-0 ps-3">
                      @for(var auditSub : (java.util.List<com.github.gcolin.registration.PlayerSubscription>) mismatch.get("subs"))
                        <li>
                          ${auditSub.getNrFfe() != null ? auditSub.getNrFfe() : ""} –
                          @if(auditSub.getEvent() != null)${auditSub.getEvent().getName()}@else?@endif
                          –
                          @if(auditSub.getAmountCents() != null)
                            ${String.format(java.util.Locale.US, "%.2f", auditSub.getAmountCents() / 100.0)} €
                          @else
                            <em>${msg.get("paymentAudit.undefinedAmount")}</em>
                          @endif
                        </li>
                      @endfor
                    </ul>
                  @else
                    <em class="text-muted">${msg.get("paymentAudit.noSubs")}</em>
                  @endif
                </td>
                <td>
                  @if(mismatch.get("subs") != null && !((java.util.List<?>) mismatch.get("subs")).isEmpty() && mismatch.get("payment") != null)
                    <form method="post" action="${contextPath}/payment/audit/fix/${((com.github.gcolin.payment.Payment) mismatch.get("payment")).getId()}">
                      @if(seasonId != null)
                        <input type="hidden" name="seasonId" value="${seasonId}">
                      @endif
                      <button type="submit" class="btn btn-sm btn-outline-warning">${msg.get("action.fix")}</button>
                    </form>
                  @endif
                </td>
              </tr>
            @endfor
          </tbody>
        </table>
      `
      )
    @else
      <div class="alert alert-success">${msg.get("paymentAudit.allOk")}</div>
    @endif
  `
)
