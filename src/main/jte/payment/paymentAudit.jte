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
!{i18ntitle = "Audit des paiements";}
!{title = i18ntitle;}
!{page = "payments";}
!{if (seasons == null) seasons = java.util.List.of();}
!{String seasonParam = seasonId != null ? "?seasonId=" + seasonId : "";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/payment">${msg.get("breadcrumb.payments")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">Audit</li>
      </ol>
    </nav>

    <div class="mb-2 bg-body rounded px-3 py-2">
      <form method="get" action="${contextPath}/payment/audit" class="row g-2 align-items-end mb-3">
        @template.platform.seasonFilter(msg=msg, seasons=seasons, seasonId=seasonId, selectId="audit_season", autoSubmit=true)
      </form>

      <h5 class="mb-3">Audit des montants</h5>

      @if(mismatches != null && !mismatches.isEmpty())
        <p class="text-body-secondary">
          ${mismatches.size()} paiement(s) avec un écart et/ou des inscriptions sans montant (tarif jeune inclus).
        </p>
        <div class="table-responsive">
          <table class="table table-striped align-middle">
            <thead>
              <tr>
                <th>#</th>
                <th>Email</th>
                <th>Montant paiement</th>
                <th>Somme attendue</th>
                <th>Somme actuelle</th>
                <th>Écart</th>
                <th>Inscriptions</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              @for(var mismatch : mismatches)
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
                    (paiement supérieur à l'attendu)
                  </td>
                  @elseif(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents")) < 0)
                  <td class="text-warning">
                    ${String.format(java.util.Locale.US, "%.2f", Math.abs(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents"))) / 100.0)} €
                    (paiement inférieur à l'attendu)
                  </td>
                  @elseif(mismatch.get("hasMissingAmount") != null && (Boolean) mismatch.get("hasMissingAmount"))
                  <td class="text-warning">
                    ${String.format(java.util.Locale.US, "%.2f", Math.abs(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents"))) / 100.0)} €
                    (montants d'inscription manquants)
                  </td>
                  @else
                  <td class="text-muted">
                    ${String.format(java.util.Locale.US, "%.2f", Math.abs(((Long) mismatch.get("paymentCents")) - ((Long) mismatch.get("expectedCents"))) / 100.0)} €
                    (cohérent)
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
                              <em>non défini</em>
                            @endif
                          </li>
                        @endfor
                      </ul>
                    @else
                      <em class="text-muted">Aucune inscription</em>
                    @endif
                  </td>
                  <td>
                    @if(mismatch.get("subs") != null && !((java.util.List<?>) mismatch.get("subs")).isEmpty() && mismatch.get("payment") != null)
                      <form method="post" action="${contextPath}/payment/audit/fix/${((com.github.gcolin.payment.Payment) mismatch.get("payment")).getId()}">
                        @if(seasonId != null)
                          <input type="hidden" name="seasonId" value="${seasonId}">
                        @endif
                        <button type="submit" class="btn btn-warning btn-sm">Corriger</button>
                      </form>
                    @endif
                  </td>
                </tr>
              @endfor
            </tbody>
          </table>
        </div>
      @else
        <div class="alert alert-success">Tous les paiements sont cohérents avec leurs inscriptions.</div>
      @endif
    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>
