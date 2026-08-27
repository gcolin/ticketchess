@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param Object amount
@param java.util.Map<String, Double> amountByPaymentType
@param java.util.Map<String, Integer> byCategory
@param java.util.Map<String, Integer> byClub
@param java.util.Map<String, Integer> byFederation
@param Object club
@param Object entry
@param com.github.gcolin.event.Event event
@param java.util.List<com.github.gcolin.event.Event> events
@param String femalePct
@param String malePct
@param Object p
@param com.github.gcolin.payment.Payment payment
@param java.util.List<com.github.gcolin.payment.Payment> payments
@param java.util.List<java.util.Map<String, Object>> agePyramid
@param int agePyramidMax
@param java.util.List<java.util.Map<String, Object>> unknownPaymentPlayers
@param com.github.gcolin.player.DisplayPlayer player
@param java.util.Map<String, Object> row
@param Object statistics
@param int total
@param Double totalAmount
@param Double totalUnpaidAmount
!{title = event.getName() + " — " + msg.get("event.statistics");}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "events";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
  <main class="container flex-fill">

    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/event/${event.getId()}">${event.getName()}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("event.statistics")}</li>
      </ol>
    </nav>

    <div class="bg-body p-3 rounded mb-3 d-flex justify-content-between align-items-start flex-wrap gap-3">
      <div>
      <h2><i class="bi bi-bar-chart-line me-2"></i>${event.getName()} &mdash; ${msg.get("event.statistics")}</h2>
      <p class="text-body-secondary">${msg.get("statistics.event.total", total)}</p>
      <p class="text-body-secondary mb-2">${msg.get("statistics.event.totalAmount")}: <strong>${((totalAmount != null ? totalAmount : 0))} €</strong>
        @if(amountByPaymentType != null && (amountByPaymentType != null && !amountByPaymentType.isEmpty()))
          <br>
          @for(var payEntry : amountByPaymentType.entrySet())

            <span class="d-block ms-2">${msg.get("payment.entry.key." + payEntry.getKey())}: ${String.format(java.util.Locale.US, "%.2f", payEntry.getValue())} € (@if(((totalAmount != null ? totalAmount : 0)) > 0)${payEntry.getValue() * 100 / totalAmount}@else 0.0@endif%)</span>
          @endfor
        @endif
      </p>
      <p class="text-body-secondary mb-2">${msg.get("statistics.event.totalUnpaidAmount")}: <strong>${((totalUnpaidAmount != null ? totalUnpaidAmount : 0))} €</strong></p>
      <p class="text-body-secondary mb-0">${msg.get("statistics.event.totalExpected")}: <strong>${(((totalAmount != null ? totalAmount : 0)) + ((totalUnpaidAmount != null ? totalUnpaidAmount : 0)))} €</strong></p>
      </div>
      <div class="d-flex flex-column flex-sm-row gap-2 flex-shrink-0">
        <a href="${contextPath}/event/${event.getId()}/statistics/csv" class="btn btn-outline-success">
          <i class="bi bi-file-earmark-spreadsheet me-1"></i>${msg.get("statistics.event.downloadCsv")}
        </a>
        <a href="${contextPath}/event/${event.getId()}/statistics/pdf" class="btn btn-outline-danger">
          <i class="bi bi-file-earmark-pdf me-1"></i>${msg.get("statistics.event.downloadPdf")}
        </a>
        <a href="${contextPath}/event/${event.getId()}/statistics/payments/pdf" class="btn btn-outline-primary">
          <i class="bi bi-cash-coin me-1"></i>${msg.get("statistics.event.downloadPaymentsPdf")}
        </a>
      </div>
    </div>

    <div class="row g-3 mb-3">

      <!-- Par club -->
      <div class="col-12 col-lg-4">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong><i class="bi bi-shield me-1"></i> ${msg.get("statistics.event.byClub")}</strong>
            <span class="badge bg-primary">${byClub.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>${msg.get("player.club")}</th>
                    <th class="text-end">${msg.get("statistics.event.count")}</th>
                    <th class="text-end">%</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var clubEntry : byClub.entrySet())

                    <tr>
                      <td>${clubEntry.getKey()}</td>
                      <td class="text-end">${clubEntry.getValue()}</td>
                      <td class="text-end">
                        @if(total > 0)${clubEntry.getValue() * 100 / total}@else 0.0@endif%
                      </td>
                    </tr>
                  @else
                    <tr><td colspan="3" class="text-center text-muted">${msg.get("info.noData")}</td></tr>
                  @endfor
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Par catégorie -->
      <div class="col-12 col-lg-4">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong><i class="bi bi-award me-1"></i> ${msg.get("statistics.event.byCategory")}</strong>
            <span class="badge bg-success">${byCategory.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>${msg.get("player.category")}</th>
                    <th class="text-end">${msg.get("statistics.event.count")}</th>
                    <th class="text-end">%</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var catEntry : byCategory.entrySet())

                    <tr>
                      <td>${catEntry.getKey()}</td>
                      <td class="text-end">${catEntry.getValue()}</td>
                      <td class="text-end">
                        @if(total > 0)${catEntry.getValue() * 100 / total}@else 0.0@endif%
                      </td>
                    </tr>
                  @else
                    <tr><td colspan="3" class="text-center text-muted">${msg.get("info.noData")}</td></tr>
                  @endfor
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Par fédération -->
      <div class="col-12 col-lg-4">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong><i class="bi bi-flag me-1"></i> ${msg.get("statistics.event.byFederation")}</strong>
            <span class="badge bg-info">${byFederation.size()}</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-striped align-middle mb-0">
                <thead>
                  <tr>
                    <th>${msg.get("player.federation")}</th>
                    <th class="text-end">${msg.get("statistics.event.count")}</th>
                    <th class="text-end">%</th>
                  </tr>
                </thead>
                <tbody>
                  @for(var fedEntry : byFederation.entrySet())

                    <tr>
                      <td>${fedEntry.getKey()}</td>
                      <td class="text-end">${fedEntry.getValue()}</td>
                      <td class="text-end">
                        @if(total > 0)${fedEntry.getValue() * 100 / total}@else 0.0@endif%
                      </td>
                    </tr>
                  @else
                    <tr><td colspan="3" class="text-center text-muted">${msg.get("info.noData")}</td></tr>
                  @endfor
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

    </div>

    <div class="card mb-3">
      <div class="card-header d-flex justify-content-between align-items-center">
        <strong><i class="bi bi-people me-1"></i> ${msg.get("statistics.event.agePyramid")}</strong>
        <span class="badge bg-dark">${msg.get("statistics.event.ageScale2")}</span>
      </div>
      <div class="card-body">
        @if((agePyramid != null && !agePyramid.isEmpty()))
          <div class="table-responsive">
            <table class="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th class="text-end" style="width: 42%;">${msg.get("statistics.event.male")}</th>
                  <th class="text-center" style="width: 16%;">${msg.get("statistics.event.ageRange")}</th>
                  <th style="width: 42%;">${msg.get("statistics.event.female")}</th>
                </tr>
              </thead>
              <tbody>
                @for(var apRow : agePyramid)
                  <tr>
                    <td class="text-end">
                      <div class="d-flex align-items-center gap-2">
                        <span class="text-body-secondary flex-shrink-0">${String.valueOf(apRow.get("male"))}</span>
                        <div class="flex-grow-1" style="min-width: 0;">
                          <div class="bg-primary-subtle ms-auto" style="height: 0.75rem; width: ${agePyramidMax > 0 ? String.format(java.util.Locale.US, "%.2f", ((Number) apRow.get("male")).doubleValue() * 100.0 / agePyramidMax) : "0"}%; min-width: ${((Number) apRow.get("male")).intValue() > 0 ? "4px" : "0"};"></div>
                        </div>
                      </div>
                    </td>
                    <td class="text-center fw-semibold">${String.valueOf(apRow.get("label"))}</td>
                    <td>
                      <div class="d-flex align-items-center gap-2">
                        <div class="flex-grow-1" style="min-width: 0;">
                          <div class="bg-danger-subtle" style="height: 0.75rem; width: ${agePyramidMax > 0 ? String.format(java.util.Locale.US, "%.2f", ((Number) apRow.get("female")).doubleValue() * 100.0 / agePyramidMax) : "0"}%; min-width: ${((Number) apRow.get("female")).intValue() > 0 ? "4px" : "0"};"></div>
                        </div>
                        <span class="text-body-secondary flex-shrink-0">${String.valueOf(apRow.get("female"))}</span>
                      </div>
                    </td>
                  </tr>
                @endfor
              </tbody>
            </table>
          </div>
        @else
          <p class="text-body-secondary mb-0">${msg.get("info.noData")}</p>
        @endif
      </div>
    </div>

    @if(unknownPaymentPlayers != null && (unknownPaymentPlayers != null && !unknownPaymentPlayers.isEmpty()))
    <div class="card mb-3">
      <div class="card-header d-flex justify-content-between align-items-center">
        <strong><i class="bi bi-exclamation-triangle me-1"></i> ${msg.get("statistics.event.unknownPayments")}</strong>
        <span class="badge bg-warning text-dark">${unknownPaymentPlayers.size()}</span>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th>${msg.get("player.licence")}</th>
                <th>${msg.get("player.name")}</th>
                <th>${msg.get("player.firstname")}</th>
                <th class="text-end">${msg.get("payment.amount")}</th>
              </tr>
            </thead>
            <tbody>
              @for(var upp : unknownPaymentPlayers)
                <tr>
                  <td>${String.valueOf(upp.get("licence"))}</td>
                  <td>${String.valueOf(upp.get("name"))}</td>
                  <td>${String.valueOf(upp.get("firstname"))}</td>
                  <td class="text-end">${String.format(java.util.Locale.US, "%.2f", ((Number) upp.get("amount")).doubleValue())} €</td>
                </tr>
              @endfor
            </tbody>
          </table>
        </div>
      </div>
    </div>
    @endif
  </main>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>