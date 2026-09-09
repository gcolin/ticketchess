@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.payment.Payment payment
@param String suggestedPayerName
@param String returnTo
@param String success
@param Object club
@param com.github.gcolin.event.Event event
@param com.github.gcolin.membership.Membership membership
@param com.github.gcolin.notification.Notification notif
@param com.github.gcolin.membership.MembershipOption option
@param Object p
@param String status
@param Object type
!{i18ntitle = msg.get("payment.payer.editTitle", payment.getId());}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "payment-payer";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
  <main class="container flex-fill py-4">
    <div class="bg-body p-4 rounded mx-auto" style="max-width: 32rem;">
      <h1 class="h4 mb-3">${msg.get("payment.payer.editTitle", payment.getId())}</h1>
      <p class="text-body-secondary">${msg.get("payment.payer.editIntro")}</p>

      @if("payerUpdated".equals(success))
        <div class="alert alert-success" role="alert">${msg.get("payment.payer.updated")}</div>
      @endif

      <form method="post" action="${contextPath}/payment/${payment.getId()}/payer" accept-charset="UTF-8">
        <input type="hidden" name="returnTo" value="${returnTo != null ? returnTo : "/event/my"}">
        <div class="mb-3">
          <label class="form-label" for="payerName">${msg.get("payment.payerName")}</label>
          <input
            type="text"
            class="form-control"
            id="payerName"
            name="payerName"
            maxlength="255"
            value="${payment.getPayerName() != null && !payment.getPayerName().isBlank() ? payment.getPayerName() : (suggestedPayerName != null ? suggestedPayerName : "")}"
            autocomplete="name">
          <div class="form-text">${msg.get("help.paymentPayerName")}</div>
        </div>
        <div class="d-flex flex-wrap gap-2 justify-content-between">
          <a class="btn btn-outline-secondary" href="${contextPath}${returnTo != null ? returnTo : "/event/my"}">${msg.get("action.cancel")}</a>
          <div class="d-flex flex-wrap gap-2">
            <a class="btn btn-outline-success" href="${contextPath}/payment/${payment.getId()}/invoice">${msg.get("payment.downloadInvoice")}</a>
            <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
          </div>
        </div>
      </form>
    </div>
  </main>
  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>
