@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param String status
@param String manualPlayersFile
@param java.util.List<com.github.gcolin.player.ManualPlayerEntry> players
!{i18ntitle = msg.get("page.databasePlayers");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.player.ManualPlayerEntry> safePlayers = players != null ? players : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.databasePlayers"),
  breadcrumbTrail = @`
    <li class="breadcrumb-item"><a href="${contextPath}/database">${msg.get("page.database")}</a></li>
  `,
  alerts = @`
    @if(status != null)
      @if("added".equals(status))
        <div class="alert alert-success">${msg.get("databasePlayers.added")}</div>
      @elseif("deleted".equals(status))
        <div class="alert alert-success">${msg.get("databasePlayers.deleted")}</div>
      @elseif("missing".equals(status))
        <div class="alert alert-warning">${msg.get("databasePlayers.missing")}</div>
      @elseif("validation".equals(status))
        <div class="alert alert-warning">${msg.get("databasePlayers.validation")}</div>
      @elseif("error".equals(status))
        <div class="alert alert-danger">${msg.get("error.serverError")}</div>
      @endif
    @endif
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.databasePlayers"), helpText=msg.get("help.databasePlayers"), icon="bi-person-plus")

    @template.platform.adminPanel(
      heading=msg.get("databasePlayers.formTitle"),
      content = @`
        <div id="manual-player-prefill-status" class="alert d-none" role="alert"></div>
        <form method="post" action="${contextPath}/database/manual-players" class="row g-3">
          <div class="col-md-3">
            <label class="form-label" for="nrffe">${msg.get("player.licence")}</label>
            <input class="form-control" id="nrffe" name="nrffe">
          </div>
          <div class="col-md-3">
            <label class="form-label" for="fide">FIDE</label>
            <input class="form-control" id="fide" name="fide">
          </div>
          <div class="col-md-3 d-flex align-items-end">
            <button class="btn  btn-secondary w-100" type="button" id="prefill-player-btn">${msg.get("action.prefillFromId")}</button>
          </div>
          <div class="col-md-3">
            <label class="form-label" for="name">${msg.get("player.familyname")}</label>
            <input class="form-control" id="name" name="name" required>
          </div>
          <div class="col-md-3">
            <label class="form-label" for="firstname">${msg.get("player.firstname")}</label>
            <input class="form-control" id="firstname" name="firstname">
          </div>

          <div class="col-md-2">
            <label class="form-label" for="birth">${msg.get("player.birthdate")}</label>
            <input class="form-control" id="birth" name="birth" placeholder="YYYY-MM-DD">
          </div>
          <div class="col-md-2">
            <label class="form-label" for="category">${msg.get("label.category")}</label>
            <input class="form-control" id="category" name="category" placeholder="SenM">
          </div>
          <div class="col-md-2">
            <label class="form-label" for="federation">${msg.get("player.federation")}</label>
            <input class="form-control" id="federation" name="federation" placeholder="FRA">
          </div>
          <div class="col-md-2">
            <label class="form-label" for="fideTitre">${msg.get("label.fideTitle")}</label>
            <input class="form-control" id="fideTitre" name="fideTitre" placeholder="m,f,g...">
          </div>
          <div class="col-md-4">
            <label class="form-label" for="club">${msg.get("player.club")}</label>
            <input class="form-control" id="club" name="club">
          </div>

          <div class="col-md-4">
            <label class="form-label" for="eloStd">Elo standard</label>
            <input class="form-control" id="eloStd" name="eloStd" placeholder="1399F">
          </div>
          <div class="col-md-4">
            <label class="form-label" for="eloRapide">Elo rapide</label>
            <input class="form-control" id="eloRapide" name="eloRapide" placeholder="1399F">
          </div>
          <div class="col-md-4">
            <label class="form-label" for="eloBlitz">Elo blitz</label>
            <input class="form-control" id="eloBlitz" name="eloBlitz" placeholder="1399F">
          </div>

          <div class="col-12">
            <button class="btn btn-primary" type="submit">${msg.get("databasePlayers.addButton")}</button>
          </div>
        </form>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("databasePlayers.listTitle"),
      subheading=msg.get("databasePlayers.playerCount", safePlayers.size()),
      content = @`
      <pre class="bg-light p-2 rounded small mb-3" style="overflow:auto">${manualPlayersFile != null ? manualPlayersFile : ""}</pre>
      <div class="d-flex flex-column flex-md-row align-items-md-center justify-content-end gap-2 mb-3">
        @if(safePlayers.size() > 0)
          <div class="input-group input-group-sm" style="max-width: 360px;">
            <span class="input-group-text"><i class="bi bi-search"></i></span>
            <input id="manual-player-filter" class="form-control" type="search" placeholder="${msg.get("databasePlayers.filterPlaceholder")}" aria-label="${msg.get("databasePlayers.filterPlaceholder")}">
          </div>
        @endif
      </div>
      @if(safePlayers.size() == 0)
        <div class="text-body-secondary">${msg.get("databasePlayers.empty")}</div>
      @else
        <table id="manual-player-table" class="table table-striped table-hover mb-0">
          <thead>
            <tr>
              <th scope="col">${msg.get("player.licence")}</th>
              <th scope="col">FIDE</th>
              <th scope="col">${msg.get("player.familyname")}</th>
              <th scope="col">${msg.get("player.firstname")}</th>
              <th scope="col">${msg.get("player.birthdate")}</th>
              <th scope="col">${msg.get("player.federation")}</th>
              <th scope="col">${msg.get("player.club")}</th>
              <th scope="col">${msg.get("label.action")}</th>
            </tr>
          </thead>
          <tbody>
            @for(var mp : safePlayers)
              <tr>
                <td>${mp.getNrffe() != null ? mp.getNrffe() : ""}</td>
                <td>${mp.getFide() != null ? mp.getFide() : ""}</td>
                <td>${mp.getName() != null ? mp.getName() : ""}</td>
                <td>${mp.getFirstname() != null ? mp.getFirstname() : ""}</td>
                <td>${mp.getBirth() != null ? mp.getBirth() : ""}</td>
                <td>${mp.getFederation() != null ? mp.getFederation() : ""}</td>
                <td>${mp.getClub() != null ? mp.getClub() : ""}</td>
                <td>
                  <form method="post" action="${contextPath}/database/manual-players/delete" onsubmit="return confirm('${msg.get("action.confirm")}');">
                    <input type="hidden" name="key" value="${mp.getKey() != null ? mp.getKey() : ""}">
                    <button type="submit" class="btn btn-sm btn-outline-danger">${msg.get("action.remove")}</button>
                  </form>
                </td>
              </tr>
            @endfor
          </tbody>
        </table>
      @endif
      `
    )
  `,
  scripts = @`
    <script type="text/javascript">
      (function () {
        const prefillBtn = document.getElementById('prefill-player-btn');
        const nrffeInput = document.getElementById('nrffe');
        const fideInput = document.getElementById('fide');
        const prefillStatus = document.getElementById('manual-player-prefill-status');

        function setPrefillStatus(message, type) {
          if (!prefillStatus) {
            return;
          }
          prefillStatus.className = 'alert alert-' + type;
          prefillStatus.textContent = message;
          prefillStatus.classList.remove('d-none');
        }

        async function prefillPlayer() {
          if (!prefillBtn || !nrffeInput || !fideInput) {
            return;
          }
          const nrffe = (nrffeInput.value || '').trim();
          const fide = (fideInput.value || '').trim();
          if (!nrffe && !fide) {
            setPrefillStatus('${msg.get("databasePlayers.prefillNeedId")}', 'warning');
            return;
          }

          prefillBtn.disabled = true;
          const oldText = prefillBtn.textContent;
          prefillBtn.textContent = '${msg.get("databasePlayers.prefillLoading")}';
          try {
            const params = new URLSearchParams();
            if (nrffe) {
              params.set('nrffe', nrffe);
            }
            if (fide) {
              params.set('fide', fide);
            }
            const response = await fetch('${contextPath}/database/manual-players/prefill?' + params.toString(), {
              headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
              throw new Error('NOT_FOUND');
            }
            const player = await response.json();
            const mapping = {
              nrffe: player.nrffe,
              fide: player.fide,
              name: player.name,
              firstname: player.firstname,
              birth: player.birthDate,
              category: player.category,
              federation: player.federation,
              club: player.club,
              eloStd: player.rating,
              eloRapide: player.rapidRating,
              eloBlitz: player.blitzRating,
              fideTitre: player.fideTitre
            };
            Object.entries(mapping).forEach(([id, value]) => {
              const element = document.getElementById(id);
              if (element && value !== null && value !== undefined) {
                element.value = value;
              }
            });
            setPrefillStatus('${msg.get("databasePlayers.prefillOk")}', 'success');
          } catch (e) {
            setPrefillStatus('${msg.get("databasePlayers.prefillNotFound")}', 'warning');
          } finally {
            prefillBtn.disabled = false;
            prefillBtn.textContent = oldText;
          }
        }

        if (prefillBtn) {
          prefillBtn.addEventListener('click', prefillPlayer);
        }

        const input = document.getElementById('manual-player-filter');
        const table = document.getElementById('manual-player-table');
        if (!input || !table) {
          return;
        }

        const rows = Array.from(table.querySelectorAll('tbody tr'));

        function normalize(value) {
          return (value || '')
              .toLowerCase()
              .normalize('NFD')
              .replace(/[\u0300-\u036f]/g, '');
        }

        function applyFilter() {
          const terms = normalize(input.value).split(/\s+/).filter(Boolean);
          rows.forEach((row) => {
            const text = normalize(row.textContent);
            const match = terms.every((term) => text.includes(term));
            row.classList.toggle('d-none', !match);
          });
        }

        input.addEventListener('input', applyFilter);
      })();
    </script>
  `
)
