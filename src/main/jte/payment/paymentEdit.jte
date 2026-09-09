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
@param com.github.gcolin.payment.PaymentStatus[] paymentStatuses
@param com.github.gcolin.payment.PaymentType[] paymentTypes
@param java.util.List<com.github.gcolin.registration.SubscriptionDisplay> subscriptionDisplays
@param java.util.List<com.github.gcolin.registration.SubscriptionOptionDisplay> optionDisplays
@param java.util.List<com.github.gcolin.membership.MembershipDisplay> membershipDisplays
@param boolean donationMode
@param String success
!{boolean isDonation = donationMode || (payment != null && payment.isDonation());}
!{i18ntitle = msg.get("action.edit");}
!{title = i18ntitle;}
!{String activeTitle = payment.getId() != null
    ? (isDonation ? msg.get("payment.editDonationTitle", payment.getId()) : msg.get("payment.editTitle", payment.getId()))
    : (isDonation ? msg.get("action.newDonation") : msg.get("action.newPayment"));}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=activeTitle,
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/payment">${msg.get("breadcrumb.payments")}</a></li>
  `,
  content = @`
    @template.platform.adminPageHeader(title=activeTitle, icon=isDonation ? "bi-heart" : "bi-credit-card")

    @if("receiptSent".equals(success))
      <div class="alert alert-success mb-3" role="alert">${msg.get("payment.receiptSent")}</div>
    @endif

    @template.platform.adminPanel(content = @`
        <form id="paymentForm" method="post" action="${contextPath}/payment/save" enctype="application/x-www-form-urlencoded" accept-charset="UTF-8">
          <input type="hidden" name="id" value="@if(payment.getId() != null)${payment.getId()}@endif">
          <input type="hidden" name="toRemove" id="toRemove" value="false">
          <input type="hidden" name="donation" value="${isDonation ? "true" : "false"}">

          <div class="mb-3">
            <label class="form-label">${msg.get("payment.email")}</label>
            <input type="email" id="userEmail" name="userEmail" value="${payment.getUserEmail() != null ? payment.getUserEmail() : ""}" class="form-control" required>
          </div>

          <div class="mb-3">
            <label class="form-label" for="payerName">${isDonation ? msg.get("payment.donorName") : msg.get("payment.payerName")}</label>
            @if(isDonation)
              <input type="text" id="payerName" name="payerName" value="${payment.getPayerName() != null ? payment.getPayerName() : ""}" class="form-control" maxlength="255" required>
              <div class="form-text">${msg.get("help.paymentDonorName")}</div>
            @else
              <input type="text" id="payerName" name="payerName" value="${payment.getPayerName() != null ? payment.getPayerName() : ""}" class="form-control" maxlength="255">
              <div class="form-text">${msg.get("help.paymentPayerName")}</div>
            @endif
          </div>

          @if(isDonation)
            <div class="mb-3">
              <label class="form-label" for="payerAddress">${msg.get("payment.donorAddress")}</label>
              <textarea id="payerAddress" name="payerAddress" class="form-control" rows="3" maxlength="1000" required>${payment.getPayerAddress() != null ? payment.getPayerAddress() : ""}</textarea>
              <div class="form-text">${msg.get("help.paymentDonorAddress")}</div>
            </div>
          @endif

          <div class="mb-3">
            <label class="form-label">${msg.get("payment.status")}</label>
            <select id="status" name="status" class="form-select" required>
              @for(var payStatus : paymentStatuses)
                @if(payment.getStatus() != null && payment.getStatus() == payStatus)
                <option value="${payStatus.name()}" selected>${msg.get("payment.status." + payStatus)}</option>
                @else
                <option value="${payStatus.name()}">${msg.get("payment.status." + payStatus)}</option>
                @endif
              @endfor
            </select>
          </div>

          <div class="mb-3">
            <label class="form-label">${msg.get("payment.type")}</label>
            <select id="type" name="type" class="form-select" required>
              @for(var payType : paymentTypes)
                @if(payment.getType() != null && payment.getType() == payType)
                <option value="${payType.name()}" selected>${msg.get("payment.type." + payType)}</option>
                @else
                <option value="${payType.name()}">${msg.get("payment.type." + payType)}</option>
                @endif
              @endfor
            </select>
          </div>

          <div class="mb-3">
            <label class="form-label">${msg.get("payment.amount")}</label>
            @if(isDonation)
              <input type="number" id="amount" name="amount" value="${(payment.getAmount() != null ? payment.getAmount() : 0)}" step="0.01" min="0.01" class="form-control" required>
            @else
              <input type="number" id="amount" name="amount" value="${(payment.getAmount() != null ? payment.getAmount() : 0)}" step="0.01" min="0" class="form-control" required>
            @endif
          </div>

          @if(!isDonation)
            <div class="mb-3">
              <label class="form-label">${msg.get("payment.stripeSessionId")}</label>
              <input type="text" id="stripeSessionId" name="stripeSessionId" value="${payment.getStripeSessionId() != null ? payment.getStripeSessionId() : ""}" class="form-control">
            </div>

            <div class="mb-3">
              <label class="form-label">Intent</label>
              <input type="text" id="stripeIntent" name="stripeIntent" value="${payment.getStripeIntent() != null ? payment.getStripeIntent() : ""}" class="form-control">
            </div>

            <div class="mb-3">
              <label class="form-label">${msg.get("payment.associatedSubscription")}</label>
              <div id="subscriptionIdsContainer" class="d-flex flex-column gap-2">
                @if(subscriptionDisplays != null && subscriptionDisplays.size() > 0)
                  @for(var subDisplay : subscriptionDisplays)
                    <div class="input-group">
                      <input type="number" name="subscriptionIds" value="${subDisplay.getSubscriptionId()}" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.subscriptionId")}">
                      @if(subDisplay.getEditLink() != null && !subDisplay.getEditLink().isEmpty())
                        <a class="btn btn-outline-primary" href="${contextPath}${subDisplay.getEditLink()}">Voir</a>
                      @endif
                      <button type="button" class="btn  btn-secondary" onclick="removeSubscriptionInput(this)">${msg.get("action.remove")}</button>
                    </div>
                    @if((subDisplay.getPlayerName() != null && !subDisplay.getPlayerName().isEmpty()) || (subDisplay.getEventName() != null && !subDisplay.getEventName().isEmpty()))
                      <div class="form-text mt-1 mb-2">
                        @if(subDisplay.getPlayerName() != null && !subDisplay.getPlayerName().isEmpty())${subDisplay.getPlayerName()}@endif
                        @if((subDisplay.getPlayerName() != null && !subDisplay.getPlayerName().isEmpty()) && (subDisplay.getEventName() != null && !subDisplay.getEventName().isEmpty())) - @endif
                        @if(subDisplay.getEventName() != null && !subDisplay.getEventName().isEmpty())${subDisplay.getEventName()}@endif
                        @if(subDisplay.getEventLink() != null && !subDisplay.getEventLink().isEmpty())
                          (<a href="${contextPath}${subDisplay.getEventLink()}">${msg.get("action.view")}</a>)
                        @endif
                      </div>
                    @endif
                  @endfor
                @else
                  <div class="input-group">
                    <input type="number" name="subscriptionIds" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.subscriptionId")}">
                    <button type="button" class="btn  btn-secondary js-remove-sub" onclick="removeSubscriptionInput(this)">${msg.get("action.remove")}</button>
                  </div>
                @endif
              </div>
              <button type="button" class="btn btn-sm btn-outline-primary mt-2 js-add-sub" onclick="addSubscriptionInput()">${msg.get("action.addId")}</button>
              <div class="form-text">${msg.get("help.subscriptionIds")}</div>
            </div>

            <div class="mb-3">
              <label class="form-label">${msg.get("payment.associatedSubscriptionOption")}</label>
              <div id="optionIdsContainer" class="d-flex flex-column gap-2">
                @if(optionDisplays != null && optionDisplays.size() > 0)
                  @for(var optDisplay : optionDisplays)
                    <div class="input-group">
                      <input type="number" name="optionIds" value="${optDisplay.getOptionId()}" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.optionId")}">
                      @if(optDisplay.getEditLink() != null && !optDisplay.getEditLink().isEmpty())
                        <a class="btn btn-outline-primary" href="${contextPath}${optDisplay.getEditLink()}">Voir</a>
                      @endif
                      <button type="button" class="btn  btn-secondary" onclick="removeOptionInput(this)">${msg.get("action.remove")}</button>
                    </div>
                    <div class="form-text mt-1 mb-2">
                      @if(optDisplay.getDescription() != null && !optDisplay.getDescription().isEmpty())${optDisplay.getDescription()}@endif
                      @if(optDisplay.getAmountCents() != null) — ${optDisplay.getAmountCents() / 100.0} €@endif
                      @if(optDisplay.getStatus() != null && !optDisplay.getStatus().isEmpty()) (${msg.get("playerStatus." + optDisplay.getStatus())})@endif
                      @if((optDisplay.getPlayerName() != null && !optDisplay.getPlayerName().isEmpty()) || (optDisplay.getEventName() != null && !optDisplay.getEventName().isEmpty()))
                        <br>
                        @if(optDisplay.getPlayerName() != null && !optDisplay.getPlayerName().isEmpty())${optDisplay.getPlayerName()}@endif
                        @if((optDisplay.getPlayerName() != null && !optDisplay.getPlayerName().isEmpty()) && (optDisplay.getEventName() != null && !optDisplay.getEventName().isEmpty())) - @endif
                        @if(optDisplay.getEventName() != null && !optDisplay.getEventName().isEmpty())${optDisplay.getEventName()}@endif
                        @if(optDisplay.getEventLink() != null && !optDisplay.getEventLink().isEmpty())
                          (<a href="${contextPath}${optDisplay.getEventLink()}">${msg.get("action.view")}</a>)
                        @endif
                      @endif
                    </div>
                  @endfor
                @else
                  <div class="input-group">
                    <input type="number" name="optionIds" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.optionId")}">
                    <button type="button" class="btn  btn-secondary" onclick="removeOptionInput(this)">${msg.get("action.remove")}</button>
                  </div>
                @endif
              </div>
              <button type="button" class="btn btn-sm btn-outline-primary mt-2" onclick="addOptionInput()">${msg.get("action.addId")}</button>
              <div class="form-text">${msg.get("help.optionIds")}</div>
            </div>

            <div class="mb-3">
              <label class="form-label">${msg.get("payment.associatedMembership")}</label>
              <div id="membershipIdsContainer" class="d-flex flex-column gap-2">
                @if(membershipDisplays != null && membershipDisplays.size() > 0)
                  @for(var memDisplay : membershipDisplays)
                    <div class="input-group">
                      <input type="number" name="membershipIds" value="${memDisplay.getMembershipId()}" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.membershipId")}">
                      @if(memDisplay.getEditLink() != null && !memDisplay.getEditLink().isEmpty())
                        <a class="btn btn-outline-primary" href="${contextPath}${memDisplay.getEditLink()}">Voir</a>
                      @endif
                      <button type="button" class="btn  btn-secondary" onclick="removeMembershipInput(this)">${msg.get("action.remove")}</button>
                    </div>
                    <div class="form-text mt-1 mb-2">
                      @if(memDisplay.getMemberName() != null && !memDisplay.getMemberName().isEmpty())${memDisplay.getMemberName()}@endif
                      @if(memDisplay.getAmountCents() != null) — ${memDisplay.getAmountCents() / 100.0} €@endif
                      @if(memDisplay.getStatus() != null && !memDisplay.getStatus().isEmpty()) (${msg.get("clubRegister.membershipStatus." + memDisplay.getStatus())})@endif
                    </div>
                  @endfor
                @else
                  <div class="input-group">
                    <input type="number" name="membershipIds" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.membershipId")}">
                    <button type="button" class="btn  btn-secondary" onclick="removeMembershipInput(this)">${msg.get("action.remove")}</button>
                  </div>
                @endif
              </div>
              <button type="button" class="btn btn-sm btn-outline-primary mt-2" onclick="addMembershipInput()">${msg.get("action.addId")}</button>
              <div class="form-text">${msg.get("help.membershipIds")}</div>
            </div>
          @else
            <input type="hidden" name="stripeSessionId" value="">
            <input type="hidden" name="stripeIntent" value="">
          @endif

          <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
            @if(payment.getId() != null)
              <button type="button" class="btn btn-sm btn-outline-danger js-remove-payment" onclick="removeAndSubmit()">
                ${msg.get("action.remove")}
              </button>
            @else
              <div></div>
            @endif

            <div class="d-flex gap-2">
              @if(payment.getId() != null && payment.getStatus() == com.github.gcolin.payment.PaymentStatus.PAID)
                <a class="btn btn-outline-success" href="${contextPath}/payment/${payment.getId()}/invoice">
                  ${isDonation ? msg.get("action.downloadDonationAttestation") : msg.get("action.downloadInvoice")}
                </a>
                <button type="submit"
                        class="btn btn-outline-primary"
                        formaction="${contextPath}/payment/${payment.getId()}/send-receipt"
                        formmethod="post">
                  ${isDonation ? msg.get("action.sendDonationAttestation") : msg.get("action.sendInvoice")}
                </button>
              @endif
              <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
            </div>
          </div>
        </form>
    `)
  `,
  scripts = @`
    <script type="text/javascript">
      function addSubscriptionInput() {
        const container = document.getElementById('subscriptionIdsContainer');
        if (!container) return;
        const line = document.createElement('div');
        line.className = 'input-group';
        line.innerHTML =
          '<input type="number" name="subscriptionIds" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.subscriptionId")}">'
          + '<button type="button" class="btn  btn-secondary" onclick="removeSubscriptionInput(this)">${msg.get("action.remove")}</button>';
        const input = line.querySelector('input');
        input.value = '';
        container.appendChild(line);
      }

      function removeSubscriptionInput(button) {
        const container = document.getElementById('subscriptionIdsContainer');
        if (container.children.length === 1) {
          const input = container.querySelector('input[name="subscriptionIds"]');
          if (input) {
            input.value = '';
          }
          return;
        }
        button.closest('.input-group').remove();
      }

      function addOptionInput() {
        const container = document.getElementById('optionIdsContainer');
        if (!container) return;
        const line = document.createElement('div');
        line.className = 'input-group';
        line.innerHTML =
          '<input type="number" name="optionIds" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.optionId")}">'
          + '<button type="button" class="btn  btn-secondary" onclick="removeOptionInput(this)">${msg.get("action.remove")}</button>';
        const input = line.querySelector('input');
        input.value = '';
        container.appendChild(line);
      }

      function removeOptionInput(button) {
        const container = document.getElementById('optionIdsContainer');
        const group = button.closest('.input-group');
        if (container.querySelectorAll('input[name="optionIds"]').length === 1) {
          const input = group.querySelector('input[name="optionIds"]');
          if (input) {
            input.value = '';
          }
          return;
        }
        group.remove();
      }

      function addMembershipInput() {
        const container = document.getElementById('membershipIdsContainer');
        if (!container) return;
        const line = document.createElement('div');
        line.className = 'input-group';
        line.innerHTML =
          '<input type="number" name="membershipIds" min="1" step="1" class="form-control" placeholder="${msg.get("placeholder.membershipId")}">'
          + '<button type="button" class="btn  btn-secondary" onclick="removeMembershipInput(this)">${msg.get("action.remove")}</button>';
        const input = line.querySelector('input');
        input.value = '';
        container.appendChild(line);
      }

      function removeMembershipInput(button) {
        const container = document.getElementById('membershipIdsContainer');
        const group = button.closest('.input-group');
        if (container.querySelectorAll('input[name="membershipIds"]').length === 1) {
          const input = group.querySelector('input[name="membershipIds"]');
          if (input) {
            input.value = '';
          }
          return;
        }
        group.remove();
      }

      function removeAndSubmit() {
        if (confirm('${msg.get("sub.confirmDelete")}')) {
          document.getElementById('toRemove').value = 'true';
          document.getElementById('paymentForm').submit();
        }
      }
    </script>
  `
)
