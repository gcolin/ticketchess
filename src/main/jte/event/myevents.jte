@param String contextPath
@param String error
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param boolean ribAvailable
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param Object club
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventCollection eventCollection
@param java.util.List<com.github.gcolin.event.Event> events
@param java.util.List<com.github.gcolin.event.Event> pendingEvents
@param java.util.List<com.github.gcolin.payment.Payment> paidPayments
@param String stripePublic
@param Boolean stripeSimulated
@param boolean showFinished
@param boolean hasFinishedEvents
@param String toggleDisplayUrl
@param Object message
@param com.github.gcolin.membership.MembershipOption option
@param Object p
@param com.github.gcolin.payment.Payment payment
@param com.github.gcolin.player.DisplayPlayer player
@param java.util.List<com.github.gcolin.player.Player> players
@param com.github.gcolin.membership.LicensePrice price
@param String status
@param Object type
!{i18ntitle = msg.get("myevents.title");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

      <html>


      <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "myevents";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
        <main class="container flex-fill">
          @if(success != null && "register".equals(success))
            <div class="alert alert-success" role="alert">${msg.get("register.success")}</div>
          @elseif(success != null && "pending".equals(success))
            <div class="alert alert-info" role="alert">${msg.get("register.pending.success")}</div>
          @elseif(success != null && "payment".equals(success))
            <div class="alert alert-success" role="alert">${msg.get("payment.success")}</div>
          @endif

          
          @if(events.size() == 0 && pendingEvents.size() == 0)
            <div id="noevent" class="bg-body p-3 pb-1 rounded mb-3">
            <p>
              ${msg.get("event.noevent")}
            </p>
            </div>
          @endif
          @if(user.getDebt() > 0)
            <div class="bg-body p-3 rounded mb-3">
                <h2>💶
                ${msg.get("info.payment")}</h2>
                <div class="row g-3">
                  @if(stripePublic != null || (stripeSimulated != null ? stripeSimulated : false))
                  <!-- Paiement par carte -->
                  <div class="col-md-6">
                    <div class="alert alert-primary p-3 h-100">
                      <h6>💳 ${msg.get("payment.cardTitle")}</h6>
                      <p class="text-muted mb-3">
                        ${msg.get("payment.cardSecure")}
                      </p>
                      @if(stripePublic != null)
                      <a class="btn btn-primary w-100" href="javascript:void(0)" id="paynow">
                        ${msg.get("payment.payByCard")}
                      </a>
                      @else
                      <a class="btn btn-primary w-100" href="${contextPath}/payment/sim" id="paynow">
                        ${msg.get("payment.payByCard")}
                      </a>
                      @endif
                    </div>
                  </div>
                  @endif

                  @if(ribAvailable)
                  <!-- Paiement par RIB -->
                  <div class="col-md-6">
                    <div class="alert alert-primary p-3 h-100">
                      <h6>🏦 ${msg.get("payment.bankTransferTitle")}</h6>
                      <p class="text-muted mb-3">
                        ${msg.get("payment.bankTransferDesc")}
                        <br />
                        <small class="text-muted">
                          ${msg.get("payment.bankTransferNote")}
                        </small>
                      </p>

                      <a href="${contextPath}/rib" class="btn btn-primary w-100" target="_blank">
                        ${msg.get("payment.downloadRIB")}
                      </a>
                    </div>
                  </div>
                  @endif

                </div>

                <!-- Autre option -->
                <div class="alert alert-info mt-4 mb-0">
                  💡 <strong>${msg.get("payment.otherMeansTitle")}</strong>
                  ${msg.get("payment.otherMeansDesc")}
                  <br>
                  <small class="text-muted">
                    ${msg.get("payment.otherMeansNote")}
                  </small>
                  <p class="text-muted small mt-2">
                    ⚠️ ${msg.get("payment.priceEvolution")}
                  </p>
                </div>

            </div>
            @if(stripePublic != null)
            <script type="text/javascript">

              const form = document.getElementById('paynow');

              let stripePromise = null;

              function loadStripe() {
                if (stripePromise) {
                  return stripePromise; // déjà chargé ou en cours
                }

                stripePromise = new Promise((resolve, reject) => {
                  if (window.Stripe) {
                    resolve(window.Stripe);
                    return;
                  }

                  const script = document.createElement('script');
                  script.src = "https://js.stripe.com/v3/";
                  script.async = true;

                  script.onload = () => resolve(window.Stripe);
                  script.onerror = () => reject(new Error("${msg.get("error.stripeLoadError")}"));

                  document.head.appendChild(script);
                });

                return stripePromise;
              };

              form.addEventListener('click', async () => {
                form.disabled = true;

                try {
                  const res = await fetch('${contextPath}/payment/initiate', {
                    method: 'POST'
                  });

                  if (!res.ok) {
                    form.disabled = false;
                    alert('${msg.get("error.serverError")}');
                    return;
                  }

                  const data = await res.json();

                  if(!data['sessionId']) {
                  	window.location.reload();
                  	return;
                  }

                  const StripeLib = await loadStripe();
                  const stripe = StripeLib('${stripePublic != null ? stripePublic : ""}');

                  const result = await stripe.redirectToCheckout({
                    sessionId: data.sessionId
                  });

                  if (result.error) {
                    form.disabled = false;
                    alert(result.error.message);
                  }
                } catch (e) {
                  form.disabled = false;
                  alert('${msg.get("error.prefix")}' + e);
                }
              });
            </script>
            @endif
          @endif

          
            @for(var ev : events)
              <div class="bg-body p-3 rounded mb-3">
            <h3><a href="${contextPath}/event/${ev.getId()}">${ev.getName()}</a></h3>
            <div class="rounded" style="overflow: hidden;"> 
            <table class="table table-striped mb-0">
              <thead>
                <tr>
                  <th scope="col" class="d-none d-md-table-cell">${msg.get("player.licence")}</th>
                  <th scope="col">
                    ${msg.get("player.name")}
                  </th>
                  <th scope="col" class="d-none d-md-table-cell">
                    ${msg.get("player.category")}
                  </th>
                  <th scope="col" class="d-none d-md-table-cell">
                    ${msg.get("player.rating")}
                  </th>
                  <th class="d-none d-md-table-cell">${msg.get("player.club")}</th>
                  @if(!ev.isFree())
                  <th scope="col" class="d-none d-md-table-cell">
                    ${msg.get("event.price")}
                  </th>
                  <th scope="col">
                    ${msg.get("event.status")}
                  </th>
                  @endif
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for(var pl : ev.getPlayers())
                  <tr>
                    <td scope="row" class="d-none d-md-table-cell">
                      ${pl.getLicence() != null ? pl.getLicence() : ""}
                    </td>
                    <td class="js-name">
                      ${pl.getName() != null ? pl.getName() : ""}
                      ${pl.getFirstname() != null ? pl.getFirstname() : ""}
                    </td>
                    <td class="d-none d-md-table-cell">
                      ${pl.getCategory() != null ? pl.getCategory() : ""}
                    </td>
                    <td class="d-none d-md-table-cell">
                      ${pl.getRating() != null ? pl.getRating() : ""}
                    </td>
                    <td class="d-none d-md-table-cell">${pl.getClub() != null ? pl.getClub() : ""}</td>
                    @if(!ev.isFree())
                    <td class="js-price d-none d-md-table-cell">
                      ${pl.getPrice() != null ? String.valueOf(pl.getPrice()) : ""}
                    </td>
                    <td class="js-status">@if(pl.getStatus() != null)${msg.get("playerStatus." + pl.getStatus().name())}@else-@endif</td>
                    @endif
                    <td>
                    @if(ev.getStatus() != null && "ACTIVE".equals(ev.getStatus().name()))
                    @if(ev.isPointageEnabled() && pl.getStatus() != null && "PAID".equals(pl.getStatus().name()))
                      @if(pl.getAttendanceAt() != null)
                        <span class="badge text-bg-success">${msg.get("action.pointed")}</span>
                      @else
                        <a class="btn btn-outline-success btn-sm"
                           href="${contextPath}/event/${ev.getId()}/pointe?subId=${pl.getSubId()}"
                           onclick="return confirm('${msg.get("action.confirm")}')">
                          ${msg.get("action.pointe")}</a>
                      @endif
                    @endif
                    @if(pl.getStatus() != null && !"CANCELLED".equals(pl.getStatus().name()))
                    <a class="btn btn-outline-danger btn-sm"
                        href="${contextPath}/event/${ev.getId()}/unregister?nrffe=${pl.getNrffeId() != null ? pl.getNrffeId() : ""}"
                        onclick="return confirm('${msg.get("action.confirm")}')">
                      ${msg.get("action.unregister")}</a>
                     @endif
                    @if(ev.getEventCollection() != null)
                    <a class="btn btn-outline-info btn-sm"
                        href="${contextPath}/event/my/${pl.getSubId()}/eventcollection/${ev.getEventCollection().getId()}">
                      ${msg.get("action.change")}</a>
                    @endif
                    @endif
                     @if(pl.isEditable())
                     <a class="js-edit btn btn-outline-info btn-sm"
                        href="${contextPath}/customplayer/${pl.getId()}">${msg.get("action.edit")}</a>
                     @endif
                    </td>
                  </tr>
                @endfor
              </tbody>
            </table>
            </div>
            </div>
            @endfor

          @if(pendingEvents.size() > 0)
            <div class="bg-body p-3 rounded mb-3">
              <h2>${msg.get("myevents.pendingQueue.title")}</h2>
            </div>
            @for(var pev : pendingEvents)
              <div class="bg-body p-3 rounded mb-3">
                <h3><a href="${contextPath}/event/${pev.getId()}">${pev.getName()}</a></h3>
                <div class="rounded" style="overflow: hidden;">
                  <table class="table table-striped mb-0">
                    <thead>
                      <tr>
                        <th scope="col" class="d-none d-md-table-cell">${msg.get("player.licence")}</th>
                        <th scope="col">${msg.get("player.name")}</th>
                        <th scope="col" class="d-none d-md-table-cell">${msg.get("player.category")}</th>
                        <th scope="col" class="d-none d-md-table-cell">${msg.get("player.rating")}</th>
                        <th class="d-none d-md-table-cell">${msg.get("player.club")}</th>
                        <th scope="col">${msg.get("myevents.pendingQueue.position")}</th>
                        <th scope="col">${msg.get("myevents.pendingQueue.status")}</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      @for(var pl : pev.getPlayers())
                        <tr>
                          <td scope="row" class="d-none d-md-table-cell">${pl.getLicence() != null ? pl.getLicence() : ""}</td>
                          <td class="js-name">${pl.getName() != null ? pl.getName() : ""} ${pl.getFirstname() != null ? pl.getFirstname() : ""}</td>
                          <td class="d-none d-md-table-cell">${pl.getCategory() != null ? pl.getCategory() : ""}</td>
                          <td class="d-none d-md-table-cell">${pl.getRating() != null ? pl.getRating() : ""}</td>
                          <td class="d-none d-md-table-cell">${pl.getClub() != null ? pl.getClub() : ""}</td>
                          <td>${msg.get("myevents.pendingQueue.ahead", (pl.getPendingQueueAhead() != null ? pl.getPendingQueueAhead() : 0))}</td>
                          <td>${msg.get("myevents.pendingQueue.status")}</td>
                          <td>
                            <a class="btn btn-outline-danger btn-sm"
                               href="${contextPath}/event/${pev.getId()}/unregister-pending?nrffe=${pl.getNrffeId() != null ? pl.getNrffeId() : ""}"
                               onclick="return confirm('${msg.get("action.confirm")}')">
                              ${msg.get("action.leavePendingQueue")}
                            </a>
                            @if(pl.isEditable())
                              <a class="js-edit btn btn-outline-info btn-sm"
                                 href="${contextPath}/customplayer/${pl.getId()}">${msg.get("action.edit")}</a>
                            @endif
                          </td>
                        </tr>
                      @endfor
                    </tbody>
                  </table>
                </div>
              </div>
            @endfor
          @endif
            
            
          @if(hasFinishedEvents)
          <div class="mb-3">
            <a class="btn btn-info" href="${toggleDisplayUrl}">
              @if(showFinished)${msg.get("myevents.hideFinished")}@else${msg.get("myevents.showFinished")}@endif
            </a>
          </div>
          @endif
            
          @if(paidPayments.size() > 0)
            <div class="bg-body p-3 rounded mb-3 d-flex align-items-center justify-content-between">
              <span>${msg.get("payment.invoiceReady")}</span>
              <div class="d-flex flex-wrap gap-2 justify-content-end">
                @for(var pp : paidPayments)
                  <a class="btn btn-outline-success btn-sm" href="${contextPath}/payment/${pp.getId()}/invoice">
                    📄 ${msg.get("payment.downloadInvoice")} #${pp.getId()}
                  </a>
                @endfor
              </div>
            </div>
          @endif

        </main>
        @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
      </body>

      </html>