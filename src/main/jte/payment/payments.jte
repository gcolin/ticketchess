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
!{i18ntitle = msg.get("page.payments");}
!{title = i18ntitle;}
!{if (payments == null) payments = java.util.List.of();}
!{if (paymentSearchNames == null) paymentSearchNames = java.util.Map.of();}
!{if (paymentSearchLicences == null) paymentSearchLicences = java.util.Map.of();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "payments";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("breadcrumb.payments")}</li>
      </ol>
    </nav>



    <div class="mb-2 bg-body rounded px-3 py-2">
      <div class="d-flex gap-2 mb-3">
        <a class="btn btn-success" href="${contextPath}/payment/new">${msg.get("action.newPayment")}</a>
        <a class="btn btn-dark" href="${contextPath}/payment/export/csv">${msg.get("action.exportCsv")}</a>
        <a class="btn btn-outline-dark" href="${contextPath}/payment/export" target="_blanc">${msg.get("action.exportJson")}</a>
        <a class="btn btn-outline-warning" href="${contextPath}/payment/audit">Audit montants</a>
	    </div>
      <form method="get" action="${contextPath}/payment" class="row g-2 mb-3">
      <div class="col-12 col-md-6">
        <input type="text" name="search" id="paymentSearch" class="form-control" placeholder="Recherche par email ou licence" autocomplete="off" value="${search != null ? search : ""}">
      </div>
      <div class="col-12 col-md-3">
        <button type="submit" class="btn btn-outline-dark">Rechercher</button>
      @if(search != null && !search.isEmpty())
        <a class="btn btn-outline-secondary" href="${contextPath}/payment?size=${pageSize}">Effacer</a>
      @endif
      </div>
      <div class="col-12 col-md-3">
        <select id="paymentSort" class="form-select" aria-label="Tri des paiements">
          <option value="id">Trier par ID</option>
          <option value="name">Trier par nom</option>
        </select>
      </div>
      <input type="hidden" name="size" value="${pageSize}">
      </form>
    <div class="d-flex justify-content-between align-items-center">
      <div class="text-body-secondary">
        ${msg.get("pagination.paymentCount", totalItems, currentPage, totalPages)}
      </div>
      <div class="d-flex gap-2">
        @if(hasPrev)
          @if(search != null && !search.isEmpty())
          <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${prevPage}&size=${pageSize}&search=${java.net.URLEncoder.encode(search, java.nio.charset.StandardCharsets.UTF_8)}">${msg.get("pagination.previous")}</a>
          @else
          <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${prevPage}&size=${pageSize}">${msg.get("pagination.previous")}</a>
          @endif
        @else
          <span class="btn btn-outline-tertiary btn-sm disabled">${msg.get("pagination.previous")}</span>
        @endif
        @if(hasNext)
          @if(search != null && !search.isEmpty())
          <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${nextPage}&size=${pageSize}&search=${java.net.URLEncoder.encode(search, java.nio.charset.StandardCharsets.UTF_8)}">${msg.get("pagination.next")}</a>
          @else
          <a class="btn btn-outline-dark btn-sm" href="${contextPath}/payment?page=${nextPage}&size=${pageSize}">${msg.get("pagination.next")}</a>
          @endif
        @else
          <span class="btn btn-outline-tertiary btn-sm disabled">${msg.get("pagination.next")}</span>
        @endif
      </div>
      </div>
    </div>

    <div class="table-responsive">
      <table class="table table-striped align-middle" id="paymentsTable">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">${msg.get("pay.email")}</th>
            <th scope="col" class="d-none d-lg-table-cell">${msg.get("player.licence")}</th>
            <th scope="col" class="d-none d-lg-table-cell">${msg.get("player.name")}</th>
            <th scope="col">${msg.get("pay.status")}</th>
            <th scope="col">${msg.get("pay.type")}</th>
            <th scope="col">${msg.get("pay.amount")}</th>
            <th scope="col">${msg.get("pay.created")}</th>
            <th scope="col">${msg.get("pay.updated")}</th>
            <th scope="col">${msg.get("label.action")}</th>
          </tr>
        </thead>
        <tbody id="paymentsTableBody">
          @for(var pay : payments)
            <tr
              class="js-payment-row"
              data-id="${pay.getId()}"
              data-name="${paymentSearchNames.get(String.valueOf(pay.getId())) != null ? paymentSearchNames.get(String.valueOf(pay.getId())) : ""}">
              <td>${pay.getId()}</td>
              <td>${pay.getUserEmail() != null ? pay.getUserEmail() : ""}</td>
              <td class="d-none d-lg-table-cell">${paymentSearchLicences.get(String.valueOf(pay.getId())) != null ? paymentSearchLicences.get(String.valueOf(pay.getId())) : ""}</td>
              <td class="d-none d-lg-table-cell">${paymentSearchNames.get(String.valueOf(pay.getId())) != null ? paymentSearchNames.get(String.valueOf(pay.getId())) : ""}</td>
              <td>${pay.getStatus() != null ? pay.getStatus().name() : ""}</td>
              <td>${pay.getType() != null ? pay.getType().name() : ""}</td>
              <td>${pay.getAmount() != null ? String.format(java.util.Locale.FRANCE, "%.2f", pay.getAmount()) : "0,00"}</td>
              <td>@if(pay.getCreatedAt() != null)${pay.getCreatedAt().toString()}@endif</td>
              <td>@if(pay.getUpdatedAt() != null)${pay.getUpdatedAt().toString()}@endif</td>
              <td>
                <a class="btn btn-outline-primary btn-sm" href="${contextPath}/payment/${pay.getId()}/edit">${msg.get("action.edit")}</a>
              </td>
            </tr>
          @else
            <tr>
              <td colspan="11" class="text-center text-muted">${msg.get("info.noPayment")}</td>
            </tr>
          @endfor
        </tbody>
      </table>
    </div>

    <script type="text/javascript">
      function normalizePaymentSortValue(value) {
        return (value || '')
          .toLowerCase()
          .normalize('NFD')
          .replace(/[\u0300-\u036f]/g, '')
          .trim();
      }

      function initPaymentsSort() {
        const sortSelect = document.getElementById('paymentSort');
        const tableBody = document.getElementById('paymentsTableBody');
        const rows = Array.from(document.querySelectorAll('.js-payment-row'));

        if (!sortSelect || !tableBody || rows.length === 0) {
          return;
        }

        function sortRows() {
          const mode = sortSelect.value;
          rows.sort(function (a, b) {
            if (mode === 'name') {
              const nameA = normalizePaymentSortValue(a.getAttribute('data-name'));
              const nameB = normalizePaymentSortValue(b.getAttribute('data-name'));
              const compareByName = nameA.localeCompare(nameB, 'fr', { sensitivity: 'base' });
              if (compareByName !== 0) {
                return compareByName;
              }
            }

            const idA = Number(a.getAttribute('data-id') || '0');
            const idB = Number(b.getAttribute('data-id') || '0');
            return idB - idA;
          });

          for (const row of rows) {
            tableBody.appendChild(row);
          }
        }

        sortSelect.addEventListener('change', sortRows);
        sortRows();
      }

      initPaymentsSort();
    </script>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>