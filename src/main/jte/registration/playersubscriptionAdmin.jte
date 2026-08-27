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
@param Integer deleteFailed
@param Integer deleted
@param Integer failed
@param Integer replaced
@param int rowCount
@param int replaceableCount
@param int pendingRowCount
@param int orphanCount
@param java.util.List<java.util.Map<String, Object>> rows
@param java.util.List<java.util.Map<String, Object>> pendingRows
@param java.util.List<com.github.gcolin.player.CustomPlayer> orphanCustomPlayers
!{i18ntitle = msg.get("page.playerSubscriptionsAdmin");}
!{title = i18ntitle;}
!{page = "playersubscriptionadmin";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.playerSubscriptionsAdmin")}</li>
        </ol>
      </nav>
      <div class="card mb-3">
        <div class="card-body">
          <h1>${msg.get("heading.playerSubscriptionsAdmin")}</h1>
          <p class="lead mb-2">${msg.get("help.playerSubscriptionsAdmin")}</p>
          <p class="mb-0 text-body-secondary">${msg.get("playersub.counts", rowCount, replaceableCount)}</p>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-body">
          <h2 class="h5 mb-1">${msg.get("heading.pendingSubscriptionsAdmin")}</h2>
          <p class="mb-0 text-body-secondary">${msg.get("playersub.pendingCounts", pendingRowCount)}</p>
        </div>
      </div>

      <div class="table-responsive mb-4">
        <table class="table table-striped table-sm align-middle bg-body">
          <thead>
            <tr>
              <th>${msg.get("label.licence")}</th>
              <th>${msg.get("label.lastname")}</th>
              <th>${msg.get("label.firstname")}</th>
              <th>${msg.get("label.creationUser")}</th>
              <th>${msg.get("label.eventId")}</th>
              <th>${msg.get("label.event")}</th>
              <th>${msg.get("label.action")}</th>
            </tr>
          </thead>
          <tbody>
            @for(var pendingRow : pendingRows)
              <tr>
                <td>${pendingRow.get("licence") != null ? String.valueOf(pendingRow.get("licence")) : ""}</td>
                <td>${pendingRow.get("lastname") != null ? String.valueOf(pendingRow.get("lastname")) : ""}</td>
                <td>${pendingRow.get("firstname") != null ? String.valueOf(pendingRow.get("firstname")) : ""}</td>
                <td>${pendingRow.get("creationUser") != null ? String.valueOf(pendingRow.get("creationUser")) : ""}</td>
                <td>${String.valueOf(pendingRow.get("eventId"))}</td>
                <td><a href="${contextPath}/event/${String.valueOf(pendingRow.get("eventId"))}">${pendingRow.get("eventName") != null ? String.valueOf(pendingRow.get("eventName")) : ""}</a></td>
                <td>
                  <form method="post" action="${contextPath}/playersubscription-admin" class="m-0"
                        onsubmit="return confirm('${msg.get("action.confirm")}');">
                    <input type="hidden" name="deletePendingId" value="${String.valueOf(pendingRow.get("id"))}">
                    <button type="submit" class="btn btn-outline-danger btn-sm">${msg.get("action.remove")}</button>
                  </form>
                </td>
              </tr>
            @else
              <tr>
                <td colspan="7" class="text-center text-muted">${msg.get("playersub.noPendingSubscriptions")}</td>
              </tr>
            @endfor
          </tbody>
        </table>
      </div>

      @if(replaced != null || failed != null)
      @if((failed != null ? failed : 0) > 0)
      <div class="alert alert-warning" role="alert">
        ${msg.get("playersub.replaceResult", (replaced != null ? replaced : 0), (failed != null ? failed : 0))}
      </div>
      @else
      <div class="alert alert-success" role="alert">
        ${msg.get("playersub.replaceResult", (replaced != null ? replaced : 0), (failed != null ? failed : 0))}
      </div>
      @endif
      @endif

      @if(deleted != null || deleteFailed != null)
      @if((deleteFailed != null ? deleteFailed : 0) > 0)
      <div class="alert alert-warning" role="alert">
        ${msg.get("playersub.deleteResult", (deleted != null ? deleted : 0), (deleteFailed != null ? deleteFailed : 0))}
      </div>
      @else
      <div class="alert alert-success" role="alert">
        ${msg.get("playersub.deleteResult", (deleted != null ? deleted : 0), (deleteFailed != null ? deleteFailed : 0))}
      </div>
      @endif
      @endif

      <div class="card mb-3">
        <div class="card-body d-flex flex-column flex-sm-row justify-content-between align-items-start align-items-sm-center gap-3">
          <div>
            <h2 class="h5 mb-1">${msg.get("heading.orphanCustomPlayers")}</h2>
            <p class="mb-0 text-body-secondary">${msg.get("playersub.orphanCustomPlayers", orphanCount)}</p>
          </div>
          <form method="post" action="${contextPath}/playersubscription-admin" class="m-0">
            <input type="hidden" name="deleteOrphans" value="true">
            @if(orphanCount > 0)
            <button type="submit" class="btn btn-danger">
              ${msg.get("action.deleteOrphanCustomPlayers")}
            </button>
            @else
            <button type="submit" class="btn btn-danger" disabled>
              ${msg.get("action.deleteOrphanCustomPlayers")}
            </button>
            @endif
          </form>
        </div>
      </div>

      @if((orphanCustomPlayers != null && !orphanCustomPlayers.isEmpty()))
      <div class="table-responsive mb-3">
        <table class="table table-sm table-bordered align-middle bg-body">
          <thead>
            <tr>
              <th>${msg.get("label.customPlayer")}</th>
              <th>${msg.get("label.customPlayerLicence")}</th>
              <th>${msg.get("label.creationUser")}</th>
            </tr>
          </thead>
          <tbody>
            @for(var orphanPlayer : orphanCustomPlayers)
              <tr>
                <td>
                  <a href="${contextPath}/customplayer/${orphanPlayer.getId()}">
                    ${orphanPlayer.getFirstname() != null ? orphanPlayer.getFirstname() : ""} ${orphanPlayer.getName() != null ? orphanPlayer.getName() : ""}
                  </a>
                </td>
                <td>${orphanPlayer.getLicence() != null ? orphanPlayer.getLicence() : ""}</td>
                <td>${orphanPlayer.getCreationUser() != null ? orphanPlayer.getCreationUser() : ""}</td>
              </tr>
            @endfor
          </tbody>
        </table>
      </div>
      @else
      <div class="alert alert-secondary" role="alert">
        ${msg.get("playersub.noOrphanCustomPlayers")}
      </div>
      @endif

      <div class="d-flex justify-content-end mb-3">
        <form method="post" action="${contextPath}/playersubscription-admin">
          <input type="hidden" name="all" value="true">
          @if(replaceableCount > 0)
          <button type="submit" class="btn btn-primary">
            ${msg.get("action.replaceAllByLicence")}
          </button>
          @else
          <button type="submit" class="btn btn-primary" disabled>
            ${msg.get("action.replaceAllByLicence")}
          </button>
          @endif
        </form>
      </div>

      <div class="table-responsive">
        <table class="table table-striped table-sm align-middle bg-body">
          <thead>
            <tr>
              <th>${msg.get("label.subscription")}</th>
              <th>${msg.get("label.event")}</th>
              <th>${msg.get("label.creationUser")}</th>
              <th>${msg.get("label.currentReference")}</th>
              <th>${msg.get("label.customPlayer")}</th>
              <th>${msg.get("label.customPlayerLicence")}</th>
              <th>${msg.get("label.lucenePlayer")}</th>
              <th>${msg.get("label.replacementLicence")}</th>
              <th>${msg.get("label.status")}</th>
              <th>${msg.get("label.action")}</th>
            </tr>
          </thead>
          <tbody>
            @for(var subRow : rows)
              <tr>
                <td>${subRow.get("subscriptionId") != null ? String.valueOf(subRow.get("subscriptionId")) : ""}</td>
                <td><a href="${contextPath}/event/${String.valueOf(subRow.get("eventId"))}">${subRow.get("eventName") != null ? String.valueOf(subRow.get("eventName")) : ""}</a></td>
                <td>${subRow.get("creationUser") != null ? String.valueOf(subRow.get("creationUser")) : ""}</td>
                <td>${subRow.get("currentRef") != null ? String.valueOf(subRow.get("currentRef")) : ""}</td>
                <td>
                  @if(subRow.get("customPlayerId") != null)
                    <a href="${contextPath}/customplayer/${String.valueOf(subRow.get("customPlayerId"))}">${subRow.get("customPlayerName") != null ? String.valueOf(subRow.get("customPlayerName")) : ""}</a>
                  @else
                    ${subRow.get("customPlayerName") != null ? String.valueOf(subRow.get("customPlayerName")) : ""}
                  @endif
                </td>
                <td>${subRow.get("customPlayerLicence") != null ? String.valueOf(subRow.get("customPlayerLicence")) : ""}</td>
                <td>${subRow.get("luceneName") != null ? String.valueOf(subRow.get("luceneName")) : ""}</td>
                <td>${subRow.get("replacementNrFfe") != null ? String.valueOf(subRow.get("replacementNrFfe")) : ""}</td>
                <td>
                  @if(subRow.get("canReplace") != null && (Boolean) subRow.get("canReplace"))
                    <span class="badge bg-success">${msg.get("playersub.status.ready")}</span>
                  @else
                    <span class="badge bg-secondary">${msg.get("playersub.reason." + subRow.get("reason"))}</span>
                  @endif
                </td>
                <td>
                  @if(subRow.get("canReplace") != null && (Boolean) subRow.get("canReplace"))
                    <form method="post" action="${contextPath}/playersubscription-admin" class="m-0">
                      <input type="hidden" name="subscriptionId" value="${String.valueOf(subRow.get("subscriptionId"))}">
                      <button type="submit" class="btn btn-outline-primary btn-sm">${msg.get("action.replaceByLicence")}</button>
                    </form>
                  @endif
                </td>
              </tr>
            @else
              <tr>
                <td colspan="10" class="text-center text-muted">${msg.get("info.noSubscription")}</td>
              </tr>
            @endfor
          </tbody>
        </table>
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
  </body>
</html>