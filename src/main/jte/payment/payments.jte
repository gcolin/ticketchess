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
@param com.github.gcolin.membership.MembershipOption option
@param java.util.Map<String, String> paymentSearchNames
@param java.util.Map<String, String> paymentSearchLicences
@param java.util.List<com.github.gcolin.payment.Payment> payments
@param String search
@param int pageSize
@param long totalItems
@param int currentPage
@param int totalPages
@param boolean hasPrev
@param boolean hasNext
@param int prevPage
@param int nextPage
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.payments");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.payment.Payment> safePayments = payments != null ? payments : java.util.List.of();}
!{java.util.Map<String, String> safePaymentSearchNames = paymentSearchNames != null ? paymentSearchNames : java.util.Map.of();}
!{java.util.Map<String, String> safePaymentSearchLicences = paymentSearchLicences != null ? paymentSearchLicences : java.util.Map.of();}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
!{String seasonParam = seasonId != null ? "&seasonId=" + seasonId : "";}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("breadcrumb.payments"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.payments"), helpText=msg.get("admin.tile.paymentsDesc"), icon="bi-credit-card")

    @template.platform.adminPanel(content = @`
      <div class="d-flex justify-content-between align-items-center gap-2 mb-3 w-100">
        <a class="btn btn-success" href="${contextPath}/payment/new">${msg.get("action.newPayment")}</a>
        @template.payment.paymentPageActionsMenu(contextPath=contextPath, msg=msg, seasonId=seasonId)
      </div>
      <form method="get" action="${contextPath}/payment" class="row g-2 align-items-end mb-3">
        <input type="hidden" name="page" value="0">
        <input type="hidden" name="size" value="${pageSize}">
        <div class="col-auto">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="payment_season", autoSubmit=true, inline=true)
        </div>
        <div class="col-12 col-lg">
          <label for="paymentSearch" class="form-label mb-0">${msg.get("payment.search.label")}</label>
          <input type="search" name="search" id="paymentSearch" class="form-control" placeholder="${msg.get("payment.search.placeholder")}" autocomplete="off" value="${search != null ? search : ""}">
        </div>
        <div class="col-12 col-sm-6 col-lg-auto">
          <label for="paymentSort" class="form-label mb-0">${msg.get("payment.sort.label")}</label>
          <select id="paymentSort" class="form-select">
            <option value="id">${msg.get("payment.sort.idDesc")}</option>
            <option value="name">${msg.get("payment.sort.nameAsc")}</option>
          </select>
        </div>
        <div class="col-12 col-sm-6 col-lg-auto">
          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-outline-dark flex-grow-1">${msg.get("action.search")}</button>
            @if(search != null && !search.isEmpty())
              <a class="btn  btn-secondary" href="${contextPath}/payment?size=${pageSize}${seasonParam}">${msg.get("action.clear")}</a>
            @endif
          </div>
        </div>
      </form>
      <div class="d-flex justify-content-between align-items-center">
        <div class="text-body-secondary">
          ${msg.get("pagination.paymentCount", totalItems, currentPage, totalPages)}
        </div>
        <div class="d-flex gap-2">
          @if(hasPrev)
            @if(search != null && !search.isEmpty())
              <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${prevPage}&size=${pageSize}&search=${java.net.URLEncoder.encode(search, java.nio.charset.StandardCharsets.UTF_8)}${seasonParam}">${msg.get("pagination.previous")}</a>
            @else
              <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${prevPage}&size=${pageSize}${seasonParam}">${msg.get("pagination.previous")}</a>
            @endif
          @else
            <span class="btn  btn-secondary btn-sm disabled">${msg.get("pagination.previous")}</span>
          @endif
          @if(hasNext)
            @if(search != null && !search.isEmpty())
              <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${nextPage}&size=${pageSize}&search=${java.net.URLEncoder.encode(search, java.nio.charset.StandardCharsets.UTF_8)}${seasonParam}">${msg.get("pagination.next")}</a>
            @else
              <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${nextPage}&size=${pageSize}${seasonParam}">${msg.get("pagination.next")}</a>
            @endif
          @else
            <span class="btn  btn-secondary btn-sm disabled">${msg.get("pagination.next")}</span>
          @endif
        </div>
      </div>
      <table class="table table-striped align-middle mb-0 mt-3" id="paymentsTable">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("payment.email")}</th>
            <th scope="col" class="d-none d-lg-table-cell">${msg.get("player.licence")}</th>
            <th scope="col" class="d-none d-lg-table-cell">${msg.get("player.name")}</th>
            <th scope="col">${msg.get("payment.status")}</th>
            <th scope="col">${msg.get("payment.type")}</th>
            <th scope="col">${msg.get("payment.amount")}</th>
            <th scope="col">${msg.get("payment.created")}</th>
            <th scope="col">${msg.get("payment.updated")}</th>
            <th scope="col">${msg.get("label.action")}</th>
          </tr>
        </thead>
        <tbody id="paymentsTableBody">
          @for(var pay : safePayments)
            <tr class="js-payment-row" data-id="${pay.getId()}" data-name="${safePaymentSearchNames.get(String.valueOf(pay.getId())) != null ? safePaymentSearchNames.get(String.valueOf(pay.getId())) : ""}">
              <td>${pay.getId()}</td>
              <td>${pay.getUserEmail() != null ? pay.getUserEmail() : ""}</td>
              <td class="d-none d-lg-table-cell">${safePaymentSearchLicences.get(String.valueOf(pay.getId())) != null ? safePaymentSearchLicences.get(String.valueOf(pay.getId())) : ""}</td>
              <td class="d-none d-lg-table-cell">${safePaymentSearchNames.get(String.valueOf(pay.getId())) != null ? safePaymentSearchNames.get(String.valueOf(pay.getId())) : ""}</td>
              <td>${pay.getStatus() != null ? msg.get("payment.status." + pay.getStatus()) : ""}</td>
              <td>${pay.getType() != null ? msg.get("payment.type." + pay.getType()) : ""}</td>
              <td>${pay.getAmount() != null ? String.format(java.util.Locale.FRANCE, "%.2f", pay.getAmount()) : "0,00"}</td>
              <td>@if(pay.getCreatedAt() != null)${pay.getCreatedAt().toString()}@endif</td>
              <td>@if(pay.getUpdatedAt() != null)${pay.getUpdatedAt().toString()}@endif</td>
              <td>
                @template.payment.paymentRowActionsMenu(contextPath=contextPath, msg=msg, payment=pay)
              </td>
            </tr>
          @else
            <tr>
              <td colspan="10" class="text-center text-body-secondary">${msg.get("info.noPayment")}</td>
            </tr>
          @endfor
        </tbody>
      </table>
    `)
  `,
  scripts = @`
    <script type="text/javascript">
      function normalizePaymentSortValue(value) {
        return (value || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
      }
      function initPaymentsSort() {
        const sortSelect = document.getElementById('paymentSort');
        const tableBody = document.getElementById('paymentsTableBody');
        const rows = Array.from(document.querySelectorAll('.js-payment-row'));
        if (!sortSelect || !tableBody || rows.length === 0) return;
        function sortRows() {
          const mode = sortSelect.value;
          rows.sort(function (a, b) {
            if (mode === 'name') {
              const nameA = normalizePaymentSortValue(a.getAttribute('data-name'));
              const nameB = normalizePaymentSortValue(b.getAttribute('data-name'));
              const compareByName = nameA.localeCompare(nameB, 'fr', { sensitivity: 'base' });
              if (compareByName !== 0) return compareByName;
            }
            return Number(b.getAttribute('data-id') || '0') - Number(a.getAttribute('data-id') || '0');
          });
          for (const row of rows) tableBody.appendChild(row);
        }
        sortSelect.addEventListener('change', sortRows);
        sortRows();
      }
      initPaymentsSort();
    </script>
  `
)
