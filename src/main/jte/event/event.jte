@param String contextPath
@param String error
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param com.github.gcolin.auth.LoggedUser user
@param Object club
@param Object eventInfo
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventCollection eventCollection
@param java.util.List<String> missingPlayers
@param com.github.gcolin.player.DisplayPlayer player
@param java.util.List<com.github.gcolin.player.DisplayPlayer> players
@param Object statistics
!{String title = event.getName() != null ? event.getName() : "";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

    <html>


    <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
        <main class="container flex-fill">

          @if(success != null && "register".equals(success))
            <div class="alert alert-success" role="alert">${msg.get("register.success")}</div>
          @endif
          @if(missingPlayers != null && !missingPlayers.isEmpty())
            <div class="alert alert-danger" role="alert">
              ${msg.get("event.playerLoadError", String.join(", ", missingPlayers))}
            </div>
          @endif

          <div class="bg-body p-3 pb-1 rounded mb-3">
            <div class="d-flex justify-content-between align-items-start gap-3">
              <h2 class="mb-0"><i class="bi bi-trophy me-2"></i>${event.getName()}
                @if(event.getFfeId() != null)
                  <a id="ffe-link" class="btn btn-outline-info btn-sm ms-2 align-middle"
                     href="https://www.echecs.asso.fr/FicheTournoi.aspx?Ref=${java.net.URLEncoder.encode(String.valueOf(event.getFfeId()), java.nio.charset.StandardCharsets.UTF_8)}"
                     target="_blank" rel="noopener noreferrer"
                     title="${msg.get("event.ffeLink")}">
                    <i class="bi bi-box-arrow-up-right me-1"></i>${msg.get("event.ffeLink")}
                  </a>
                @endif
              </h2>
              @template.event.eventActionsMenu(contextPath=contextPath, msg=msg, user=user, event=event, showView=false, showUploadPapi=true)
            </div>
            @if(event.getMaxSubscriptions() != null && Math.max(0, event.getMaxSubscriptions() - players.size()) > 0)
            <p class="text-body-secondary mb-2">${msg.get("event.remainingSubscriptions", Math.max(0, event.getMaxSubscriptions() - players.size()))}</p>
            @endif
            @if(java.util.Objects.nonNull(event.getEventCollection()) && java.util.Objects.nonNull(event.getEventCollection().getMaxSubscribe()) && java.util.Objects.nonNull(event.getEventCollection().getNbSubscriptions()) && !(event.getMaxSubscriptions() != null && Math.max(0, event.getMaxSubscriptions() - players.size()) == 0) && !(Math.max(0, event.getEventCollection().getMaxSubscribe() - event.getEventCollection().getNbSubscriptions()) == 0))
            <p class="text-body-secondary mb-2">${msg.get("event.collectionRemainingSubscriptions", Math.max(0, event.getEventCollection().getMaxSubscribe() - event.getEventCollection().getNbSubscriptions()))}</p>
            @endif

          <div class="mt-3">
            @if(event.getStatus() != null && "ACTIVE".equals(event.getStatus().name()))
              @if(user.isLogged())
                <a id="register" class="btn btn-primary mb-2" href="${contextPath}/event/${event.getId()}/register">&#9997; ${msg.get(((event.getMaxSubscriptions() != null && Math.max(0, event.getMaxSubscriptions() - players.size()) == 0) || (java.util.Objects.nonNull(event.getEventCollection()) && java.util.Objects.nonNull(event.getEventCollection().getMaxSubscribe()) && Math.max(0, event.getEventCollection().getMaxSubscribe() - event.getEventCollection().getNbSubscriptions()) == 0)) ? "action.joinPendingQueue" : "action.register")}</a>
                @if((event.getMaxSubscriptions() != null && Math.max(0, event.getMaxSubscriptions() - players.size()) == 0) || (java.util.Objects.nonNull(event.getEventCollection()) && java.util.Objects.nonNull(event.getEventCollection().getMaxSubscribe()) && Math.max(0, event.getEventCollection().getMaxSubscribe() - event.getEventCollection().getNbSubscriptions()) == 0))
                  <p class="fw-semibold text-success mb-2">${msg.get((java.util.Objects.nonNull(event.getEventCollection()) && java.util.Objects.nonNull(event.getEventCollection().getMaxSubscribe()) && Math.max(0, event.getEventCollection().getMaxSubscribe() - event.getEventCollection().getNbSubscriptions()) == 0) ? "event.collectionFull" : "event.full")}</p>
                  <p class="text-body-secondary mb-2">${msg.get("event.pendingQueueAvailable")}</p>
                @endif
              @else
                <p><a class="btn btn-primary" id="googleauth2" href="${state.getLogin()}">${msg.get("event.loginToRegister")}</a></p>
                @if((event.getMaxSubscriptions() != null && Math.max(0, event.getMaxSubscriptions() - players.size()) == 0) || (java.util.Objects.nonNull(event.getEventCollection()) && java.util.Objects.nonNull(event.getEventCollection().getMaxSubscribe()) && Math.max(0, event.getEventCollection().getMaxSubscribe() - event.getEventCollection().getNbSubscriptions()) == 0))
                  <p class="fw-semibold text-success mb-2">${msg.get((java.util.Objects.nonNull(event.getEventCollection()) && java.util.Objects.nonNull(event.getEventCollection().getMaxSubscribe()) && Math.max(0, event.getEventCollection().getMaxSubscribe() - event.getEventCollection().getNbSubscriptions()) == 0) ? "event.collectionFull" : "event.full")}</p>
                @endif
              @endif
            @endif
            <div id="uploadPapiFeedback" class="mt-2"></div>
          </div>
          <div id="info">@if(eventInfo instanceof String)$unsafe{(String) eventInfo}@endif</div>

          </div>

		<div class="rounded" style="overflow: hidden;"> 
          <table id="players-table" class="table table-striped mb-0">
            <thead>
              <tr>
                <th scope="col"></th>
                <th scope="col" class="d-none d-md-table-cell">
                  <a id="sort-by-licence" class="js-sort-link" href="javascript:void(0)" data-sort-key="licence">
                    <i class="bi bi-person-vcard me-2"></i> ${msg.get("player.licence")} <span class="js-sort-icon" aria-hidden="true"></span>
                  </a>
                </th>
                <th scope="col">
                  <a id="sort-by-name" class="js-sort-link" href="javascript:void(0)" data-sort-key="name">
                    <i class="bi bi-person me-2"></i> ${msg.get("player.name")} <span class="js-sort-icon" aria-hidden="true"></span>
                  </a>
                </th>
                <th scope="col" class="d-none d-md-table-cell">
                  <a id="sort-by-category" class="js-sort-link" href="javascript:void(0)" data-sort-key="category">
                    <i class="bi bi-award me-2"></i> ${msg.get("player.category")} <span class="js-sort-icon" aria-hidden="true"></span>
                  </a>
                </th>
                <th scope="col">
                    <a id="sort-by-rating" class="js-sort-link" href="javascript:void(0)" data-sort-key="rating">
                    <i class="bi bi-bar-chart me-2"></i> ${msg.get("player.rating")} <span class="js-sort-icon" aria-hidden="true"></span>
                  </a>
                </th>
                <th class="d-none d-md-table-cell">
                  <a id="sort-by-club" class="js-sort-link" href="javascript:void(0)" data-sort-key="club">
                    <i class="bi bi-shield me-2"></i> ${msg.get("player.club")} <span class="js-sort-icon" aria-hidden="true"></span>
                  </a>
                </th>
                @if(user.canManageEvents())
                @if(!event.isFree())
                <th scope="col" class="d-none d-md-table-cell">${msg.get("event.status")}</th>
                @endif
                <th scope="col"></th>
                @endif
              </tr>
            </thead>
            <tbody>
                @for(var pl : players)
              <tr>
                <td>${players.indexOf(pl) + 1}</td>
                <td class="js-licence d-none d-md-table-cell" data-sort="${pl.getLicence() != null ? pl.getLicence() : ""}">${pl.getLicence() != null ? pl.getLicence() : ""}</td>
                <td class="js-name" data-sort="${(pl.getName() != null ? pl.getName() : "") + " " + (pl.getFirstname() != null ? pl.getFirstname() : "")}">${pl.getName()} ${pl.getFirstname()}
                @if(pl.getFideTitre() != null)<span class="badge bg-success">${msg.get("fidetitre." + pl.getFideTitre())}</span>@endif</td>
                <td class="js-category d-none d-md-table-cell" data-sort="${pl.getCategory() != null ? pl.getCategory() : ""}">${pl.getCategory()}</td>
                <td class="js-rating" data-rating="${String.valueOf(java.util.Objects.isNull(pl.getRating()) ? 0 : pl.getRating())}">${pl.getRating() != null ? pl.getRating() : ""}</td>
                <td class="js-club d-none d-md-table-cell" data-sort="${pl.getClub() != null ? pl.getClub() : ""}">${pl.getClub() != null ? pl.getClub() : ""}</td>
                 @if(user.canManageEvents())
                @if(!event.isFree())
                <td class="js-status d-none d-md-table-cell">
                   ${msg.get("playerStatus." + pl.getStatus().name())}
                </td>
                @endif
                <td>
                    <a class="btn btn-info btn-sm" href="${contextPath}/event/${event.getId()}/register/${pl.getSubId()}">${msg.get("action.edit")}</a>
                @endif
                </td>
                </tr>
              @endfor
            </tbody>
          </table>
		</div>

        <script>
          (function () {
            const table = document.getElementById('players-table');
            if (!table) {
              return;
            }

            const tbody = table.querySelector('tbody');
            const sortLinks = Array.from(table.querySelectorAll('.js-sort-link'));
            if (!tbody || sortLinks.length === 0) {
              return;
            }

            Array.from(tbody.rows).forEach(function (row, index) {
              row.setAttribute('data-original-index', String(index));
            });

            const normalizeText = function (value) {
              return (value || '')
                .toString()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .trim()
                .toLowerCase();
            };

            const getCellValue = function (row, key) {
              const cell = row.querySelector('.js-' + key);
              if (!cell) {
                return '';
              }
              return (cell.getAttribute('data-sort') || cell.textContent || '').trim();
            };

            const getRating = function (row) {
              const ratingCell = row.querySelector('.js-rating');
              if (!ratingCell) {
                return 0;
              }
              const rawRating = (ratingCell.getAttribute('data-rating') || ratingCell.textContent || '').trim();
              const normalized = rawRating
                .replace(/\s/g, '')
                .replace(',', '.')
                .replace(/[^0-9.-]/g, '');
              const parsed = Number(normalized);
              return Number.isFinite(parsed) ? parsed : 0;
            };

            const renumberRows = function () {
              Array.from(tbody.rows).forEach(function (row, index) {
                if (row.children[0]) {
                  row.children[0].textContent = String(index + 1);
                }
              });
            };

            const applySort = function (key, descending) {
              const rows = Array.from(tbody.rows);
              rows.sort(function (a, b) {
                let comparison = 0;
                if (key === 'rating') {
                  comparison = getRating(a) - getRating(b);
                } else {
                  const left = normalizeText(getCellValue(a, key));
                  const right = normalizeText(getCellValue(b, key));
                  comparison = left.localeCompare(right, 'fr', { numeric: true, sensitivity: 'base' });
                }

                if (comparison === 0) {
                  const ai = Number(a.getAttribute('data-original-index'));
                  const bi = Number(b.getAttribute('data-original-index'));
                  comparison = ai - bi;
                }

                return descending ? -comparison : comparison;
              });
              rows.forEach(function (row) {
                tbody.appendChild(row);
              });
              renumberRows();

              sortLinks.forEach(function (link) {
                const icon = link.querySelector('.js-sort-icon');
                if (!icon) {
                  return;
                }
                icon.textContent = link.getAttribute('data-sort-key') === key
                  ? (descending ? "↓" : "↑")
                  : '';
              });
            };

            let activeKey = 'rating';
            let descending = true;
            applySort(activeKey, descending);

            sortLinks.forEach(function (link) {
              link.addEventListener('click', function () {
                const key = link.getAttribute('data-sort-key');
                if (!key) {
                  return;
                }
                if (key === activeKey) {
                  descending = !descending;
                } else {
                  activeKey = key;
                  descending = false;
                }
                applySort(activeKey, descending);
              });
            });

            const uploadButton = document.getElementById('uploadPapi');
            const uploadFeedback = document.getElementById('uploadPapiFeedback');
            if (uploadButton && uploadFeedback) {
              uploadButton.addEventListener('click', async function () {
                uploadButton.disabled = true;
                uploadFeedback.className = 'alert alert-info mt-2';
                uploadFeedback.textContent = 'Upload PAPI en cours...';
                try {
                  const response = await fetch('${contextPath}/event/${event.getId()}/papi/upload', {
                    method: 'POST',
                    headers: {
                      'Accept': 'text/plain'
                    }
                  });
                  const text = await response.text();
                  if (!response.ok) {
                    throw new Error(text || 'Erreur serveur');
                  }
                  uploadFeedback.className = 'alert alert-success mt-2';
                  uploadFeedback.textContent = text || 'Upload PAPI termine';
                } catch (error) {
                  uploadFeedback.className = 'alert alert-danger mt-2';
                  uploadFeedback.textContent = error && error.message ? error.message : 'Echec upload PAPI';
                } finally {
                  uploadButton.disabled = false;
                }
              });
            }
          })();
        </script>

        <style>
          .js-sort-link,
          .js-sort-link:hover,
          .js-sort-link:focus {
            color: #fff;
            text-decoration: none;
          }
        </style>

        </main>

         @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)

    </body>

    </html>

    </html>