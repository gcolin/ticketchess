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
@param Object amount
@param Object club
@param String hasApprovedMembership
@param String hasPendingApprovalMembership
@param String hasPendingConfirmationMembership
@param com.github.gcolin.membership.Membership m
@param com.github.gcolin.membership.Membership membership
@param java.util.Map<String, java.util.List<com.github.gcolin.membership.MembershipOptionSubscription>> membershipSubscriptions
@param java.util.List<com.github.gcolin.membership.Membership> memberships
@param java.util.List<com.github.gcolin.membership.MembershipOption> options
@param Object p
@param com.github.gcolin.payment.Payment payment
@param com.github.gcolin.player.DisplayPlayer player
@param java.util.List<com.github.gcolin.player.Player> players
@param String query
@param Object search
@param String status
@param com.github.gcolin.registration.PlayerSubscription sub
@param Object type
@param String clubSeasonName
@param Double membershipDebt
@param String stripePublic
@param Boolean ribAvailable
@param Boolean stripeCardMembershipsEnabled
@param Boolean bankTransferMembershipsEnabled
@param Boolean stripeSimulated
@param java.util.List<com.github.gcolin.payment.Payment> paidMembershipPayments
!{i18ntitle = msg.get("page.clubRegister");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "clubregister";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    @if(success != null && "membership".equals(success))
      <div id="alert" class="alert alert-success" role="alert">${msg.get("clubRegister.membership.success")}</div>
    @endif
    @if(success != null && "confirmation".equals(success))
      <div id="alert" class="alert alert-success" role="alert">${msg.get("clubRegister.membership.confirmationSuccess")}</div>
    @endif
    @if(success != null && "payment".equals(success))
      <div id="alert" class="alert alert-success" role="alert">${msg.get("clubRegister.payment.success")}</div>
    @endif

    <div class="bg-body p-3 rounded mb-3" id="help" style="${user.isLogged() && memberships != null && !memberships.isEmpty() && query == null ? "display:none" : ""}">
      <h1 class="h3 mb-2">${msg.get("clubRegister.title", clubSeasonName != null ? clubSeasonName : "")}</h1>
      <p class="text-body-secondary mb-0">${msg.get("clubRegister.description", clubSeasonName != null ? clubSeasonName : "")}</p>
    </div>

    @if(user.isLogged() && query == null && memberships != null && (memberships != null && !memberships.isEmpty()) && (success == null || !"mail".equals(success)))
      <div class="mb-3" id="inscriptionButton">
        <button type="button" class="btn btn-primary" id="newInscriptionButton">${msg.get("clubRegister.newInscription")}</button>
      </div>

      <script type="text/javascript">
        document.getElementById('newInscriptionButton').addEventListener('click', function () {
          document.getElementById('inscriptionForm').style.display = 'block';
          document.getElementById('manualForm').style.display = 'block';
          document.getElementById('inscriptionButton').style.display = 'none';
          document.getElementById('membershipsSection').style.display = 'none';
          document.getElementById('alert').style.display = 'none';
          document.getElementById('help').style.display = 'block';
        });
      </script>
    @endif

    <div id="inscriptionForm" style="${user.isLogged() && memberships != null && !memberships.isEmpty() && query == null ? "display:none" : ""}">

    @if(!user.isLogged())
      <div class="bg-body p-4 rounded mb-3">
        <p class="lead mb-3">${msg.get("clubRegister.loginRequired")}</p>
        <a class="btn btn-primary" href="${state.getLogin()}">${msg.get("action.login")}</a>
      </div>
    @else
      <div class="bg-body p-3 rounded mb-3">
        <h2 class="h5 mb-3">${msg.get("clubRegister.licence.title")}</h2>
        <form method="GET" action="${contextPath}/club-register" class="mb-0">
          <div class="mb-3">
            <label for="query" class="form-label">${msg.get("search.label")}</label>
            <input type="text" class="form-control" id="query" name="query" value="${query != null ? query : ""}" />
          </div>
          <div class="d-flex align-items-center">
            <button type="submit" class="btn btn-primary">${msg.get("action.search")}</button>
          </div>
        </form>
      </div>

      @if(query != null)
        @if(players != null && players.size() > 0)
          <div class="rounded mb-3" style="overflow: hidden;">
            <table class="table table-striped mb-0">
              <thead>
                <tr>
                  <th scope="col">${msg.get("player.licence")}</th>
                  <th scope="col">${msg.get("player.name")}</th>
                  <th scope="col" class="d-none d-md-table-cell">${msg.get("player.category")}</th>
                  <th scope="col" class="d-none d-md-table-cell">${msg.get("player.rating")}</th>
                  <th scope="col" class="d-none d-md-table-cell">${msg.get("player.club")}</th>
                  <th scope="col"></th>
                </tr>
              </thead>
              <tbody>
                @for(var pl : players)
                  <tr>
                    <td>${pl.getLicence() != null ? pl.getLicence() : ""}</td>
                    <td>${pl.getName() != null ? pl.getName() : ""} ${pl.getFirstname() != null ? pl.getFirstname() : ""}</td>
                    <td class="d-none d-md-table-cell">${pl.getCategory() != null ? pl.getCategory() : ""}</td>
                    <td class="d-none d-md-table-cell">${pl.getRating() != null ? pl.getRating() : ""}</td>
                    <td class="d-none d-md-table-cell">${pl.getClub() != null ? pl.getClub() : ""}</td>
                    <td>
                      <form method="POST" action="${contextPath}/club-register/select-player">
                        <input type="hidden" name="nrffe" value="${pl.getLicence() != null ? pl.getLicence() : ""}">
                        <button type="submit" class="btn btn-sm btn-primary">${msg.get("action.select")}</button>
                      </form>
                    </td>
                  </tr>
                @endfor
              </tbody>
            </table>
          </div>
        @elseif(players != null)
          <div class="bg-body p-3 rounded mb-3">
            <p class="mb-0">${msg.get("info.noplayer")}</p>
          </div>
        @endif
      @endif

      <div class="bg-body p-3 rounded mb-3" id="manualForm">
        <h2 class="h5 mb-3">${msg.get("clubRegister.manual.title")}</h2>
        <form method="GET" action="${contextPath}/club-register/membership-options" class="mb-0">
          <div class="row g-3">
            <div class="col-12 col-md-4">
              <label for="lastname" class="form-label">${msg.get("player.familyname")}</label>
              <input type="text" class="form-control" id="lastname" name="lastname" required>
            </div>
            <div class="col-12 col-md-4">
              <label for="firstname" class="form-label">${msg.get("player.firstname")}</label>
              <input type="text" class="form-control" id="firstname" name="firstname" required>
            </div>
            <div class="col-12 col-md-4">
              <label for="birthdate" class="form-label">${msg.get("player.birthdate")}</label>
              <input type="date" class="form-control" id="birthdate" name="birthdate" required>
            </div>
          </div>
          <div class="mt-3">
            <button type="submit" class="btn btn-secondary">${msg.get("clubRegister.manual.submit")}</button>
          </div>
        </form>
      </div>
    @endif

    </div>

    @if(user.isLogged() && query == null && memberships != null && !memberships.isEmpty())
      @if(membershipDebt != null && membershipDebt > 0 && memberships.stream().anyMatch(mem -> mem.getStatus() != null && mem.getStatus() == com.github.gcolin.membership.MembershipStatus.APPROVED))
        <div class="bg-body p-3 rounded mb-3">
          <h2 class="h5 mb-3">💶 ${msg.get("clubRegister.payment.title")}</h2>
          <div class="row g-3">
            @if((stripePublic != null || (stripeSimulated != null ? stripeSimulated : false)) && (stripeCardMembershipsEnabled != null ? stripeCardMembershipsEnabled : true))
            <div class="col-md-6">
              <div class="alert alert-primary p-3 h-100">
                <h6>💳 ${msg.get("payment.cardTitle")}</h6>
                <p class="text-muted mb-3">${msg.get("payment.cardSecure")}</p>
                @if(stripePublic != null)
                <a class="btn btn-primary w-100" href="javascript:void(0)" id="paynow-membership">${msg.get("payment.payByCard")}</a>
                @else
                <a class="btn btn-primary w-100" href="${contextPath}/payment/membership/sim" id="paynow-membership">${msg.get("payment.payByCard")}</a>
                @endif
              </div>
            </div>
            @endif

            @if(ribAvailable && (bankTransferMembershipsEnabled != null ? bankTransferMembershipsEnabled : true))
            <div class="col-md-6">
              <div class="alert alert-primary p-3 h-100">
                <h6>🏦 ${msg.get("payment.bankTransferTitle")}</h6>
                <p class="text-muted mb-3">
                  ${msg.get("payment.bankTransferDesc")}
                  <br />
                  <small class="text-muted">${msg.get("payment.bankTransferNote")}</small>
                </p>
                <a href="${contextPath}/rib" class="btn btn-primary w-100" target="_blank">${msg.get("payment.downloadRIB")}</a>
              </div>
            </div>
            @endif
          </div>

          <div class="alert alert-info mt-4 mb-0">
            💡 <strong>${msg.get("payment.otherMeansTitle")}</strong>
            ${msg.get("clubRegister.payment.otherMeansDesc")}
            <br>
            <small class="text-muted">${msg.get("payment.otherMeansNote")}</small>
          </div>
        </div>
        @if(stripePublic != null)
        <script type="text/javascript">
          const membershipPayButton = document.getElementById('paynow-membership');
          if (membershipPayButton) {
            let stripePromise = null;
            function loadStripe() {
              if (stripePromise) return stripePromise;
              stripePromise = new Promise((resolve, reject) => {
                if (window.Stripe) { resolve(window.Stripe); return; }
                const script = document.createElement('script');
                script.src = "https://js.stripe.com/v3/";
                script.async = true;
                script.onload = () => resolve(window.Stripe);
                script.onerror = () => reject(new Error("${msg.get("error.stripeLoadError")}"));
                document.head.appendChild(script);
              });
              return stripePromise;
            }
            membershipPayButton.addEventListener('click', async () => {
              membershipPayButton.disabled = true;
              try {
                const res = await fetch('${contextPath}/payment/membership/initiate', { method: 'POST' });
                if (!res.ok) {
                  membershipPayButton.disabled = false;
                  alert('${msg.get("error.serverError")}');
                  return;
                }
                const data = await res.json();
                if (!data['sessionId']) {
                  window.location.reload();
                  return;
                }
                const StripeLib = await loadStripe();
                const stripe = StripeLib('${stripePublic != null ? stripePublic : ""}');
                const result = await stripe.redirectToCheckout({ sessionId: data['sessionId'] });
                if (result.error) {
                  membershipPayButton.disabled = false;
                  alert(result.error.message);
                }
              } catch (e) {
                membershipPayButton.disabled = false;
                alert('${msg.get("error.serverError")}');
              }
            });
          }
        </script>
        @endif
      @endif

      <div id="membershipsSection" class="bg-body p-3 rounded mb-3">
        <h2 class="h5 mb-3">${msg.get("clubRegister.myMemberships.title")}</h2>
          @if(memberships.stream().anyMatch(mem -> mem.getStatus() != null && mem.getStatus() == com.github.gcolin.membership.MembershipStatus.PENDING_CONFIRMATION))
            <div class="alert alert-info" role="alert"><p class="mb-0">Veuillez cliquer sur <strong>confirmer</strong> si l'adhésion convient sinon vous pouvez nous contacter pour plus d'informations.</p></div>
          @elseif(memberships.stream().anyMatch(mem -> mem.getStatus() != null && mem.getStatus() == com.github.gcolin.membership.MembershipStatus.PENDING_APPROVAL))
            <div class="alert alert-info" role="alert"><p class="mb-0">${msg.get("clubRegister.myMemberships.pendingApprovalInfo")}</p></div>
          @elseif(memberships.stream().anyMatch(mem -> mem.getStatus() != null && mem.getStatus() == com.github.gcolin.membership.MembershipStatus.APPROVED))
            <div class="alert alert-info" role="alert"><p class="mb-0">${msg.get("clubRegister.myMemberships.approvedPaymentInfo")}</p></div>
          @endif
        
        <div class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th></th>
                <th class="d-none d-md-table-cell">${msg.get("player.licence")}</th>
                <th>${msg.get("player.familyname")}</th>
                <th>${msg.get("player.firstname")}</th>
                <th>${msg.get("payment.amount")}</th>
                <th class="d-none d-md-table-cell">${msg.get("payment.created")}</th>
                <th>${msg.get("clubRegister.myMemberships.options")}</th>
                <th>${msg.get("payment.status")}</th>
              </tr>
            </thead>
            <tbody>
              @for(var mem : memberships)
                <tr>
                  <td>
                    @if(mem.getStatus() != null && mem.getStatus() == com.github.gcolin.membership.MembershipStatus.PENDING_CONFIRMATION)
                      <form method="POST" action="${contextPath}/club-register/${mem.getId()}/confirm" class="d-inline-block ms-1">
                        <button type="submit" class="btn btn-sm btn-success">${msg.get("clubRegister.membership.confirm")}</button>
                      </form>
                    @endif
                  </td>
                  <td class="d-none d-md-table-cell">${mem.getNrFfe() != null ? mem.getNrFfe() : ""}</td>
                  <td>${mem.getLastname() != null ? mem.getLastname() : ""}</td>
                  <td>${mem.getFirstname() != null ? mem.getFirstname() : ""}</td>
                  <td>${String.format(java.util.Locale.FRANCE, "%.2f", mem.getAmountCents() / 100.0)} €</td>
                  <td class="d-none d-md-table-cell">@if(mem.getCreatedAt() != null)${mem.getCreatedAt().toString()}@endif</td>
                  <td>
                    @if(membershipSubscriptions != null && membershipSubscriptions.get(String.valueOf(mem.getId())) != null)
                      @for(var mos : membershipSubscriptions.get(String.valueOf(mem.getId())))
                        <span class="badge bg-secondary me-1 text-dark">${mos.getMembershipOption() != null && mos.getMembershipOption().getOptionValue() != null ? mos.getMembershipOption().getOptionValue() : ""}</span>
                      @endfor
                    @endif
                  </td>
                  <td>${msg.get("clubRegister.membershipStatus." + mem.getStatus())}</td>
                </tr>
              @endfor
            </tbody>
          </table>
        </div>

        @if(paidMembershipPayments != null && !paidMembershipPayments.isEmpty())
          <div class="mt-3">
            <span>${msg.get("payment.invoiceReady")}</span>
            @for(var pp : paidMembershipPayments)
              <a class="btn btn-outline-success btn-sm ms-2" href="${contextPath}/payment/${pp.getId()}/invoice">
                📄 ${msg.get("payment.downloadInvoice")} #${pp.getId()}
              </a>
            @endfor
          </div>
        @endif
      </div>
    @endif
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>