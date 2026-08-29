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
!{i18ntitle = msg.get("action.edit");}
!{title = i18ntitle;}
!{String activeTitle = payment.getId() != null ? msg.get("payment.editTitle", payment.getId()) : msg.get("action.newPayment");}
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
    @template.platform.adminPageHeader(title=activeTitle, icon="bi-credit-card")

    @template.platform.adminPanel(content = @`
        <form id="paymentForm" method="post" action="${contextPath}/payment/save" enctype="application/x-www-form-urlencoded" accept-charset="UTF-8">
          <input type="hidden" name="id" value="@if(payment.getId() != null)${payment.getId()}@endif">
          <input type="hidden" name="toRemove" id="toRemove" value="false">

          <div class="mb-3">
            <label class="form-label">${msg.get("payment.email")}</label>
            <input type="email" id="userEmail" name="userEmail" value="${payment.getUserEmail() != null ? payment.getUserEmail() : ""}" class="form-control" required>
          </div>

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
            <input type="number" id="amount" name="amount" value="${(payment.getAmount() != null ? payment.getAmount() : 0)}" step="0.01" min="0" class="form-control" required>
          </div>

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

          <div class="d-flex justify-content-between">
            @if(payment.getId() != null)
              <button type="button" class="btn btn-sm btn-outline-danger js-remove-payment" onclick="removeAndSubmit()">
                ${msg.get("action.remove")}
              </button>
            @else
              <div></div>
            @endif

            <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
          </div>
        </form>
    `)
  `,
  scripts = @`
    <script type="text/javascript">
      function addSubscriptionInput() {
        const container = document.getElementById('subscriptionIdsContainer');
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

      function removeAndSubmit() {
        if (confirm('${msg.get("sub.confirmDelete")}')) {
          document.getElementById('toRemove').value = 'true';
          document.getElementById('paymentForm').submit();
        }
      }
    </script>
  `
)
