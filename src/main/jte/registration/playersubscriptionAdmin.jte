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
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.playerSubscriptionsAdmin");}
!{title = i18ntitle;}
!{java.util.List<com.github.gcolin.platform.SelectItem> safeSeasons = seasons != null ? seasons : java.util.List.of();}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.playerSubscriptionsAdmin"),
  alerts = @`
    @if(replaced != null || failed != null)
      @if((failed != null ? failed : 0) > 0)
        <div class="alert alert-warning mb-4" role="alert">
          ${msg.get("playersub.replaceResult", (replaced != null ? replaced : 0), (failed != null ? failed : 0))}
        </div>
      @else
        <div class="alert alert-success mb-4" role="alert">
          ${msg.get("playersub.replaceResult", (replaced != null ? replaced : 0), (failed != null ? failed : 0))}
        </div>
      @endif
    @endif
    @if(deleted != null || deleteFailed != null)
      @if((deleteFailed != null ? deleteFailed : 0) > 0)
        <div class="alert alert-warning mb-4" role="alert">
          ${msg.get("playersub.deleteResult", (deleted != null ? deleted : 0), (deleteFailed != null ? deleteFailed : 0))}
        </div>
      @else
        <div class="alert alert-success mb-4" role="alert">
          ${msg.get("playersub.deleteResult", (deleted != null ? deleted : 0), (deleteFailed != null ? deleteFailed : 0))}
        </div>
      @endif
    @endif
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.playerSubscriptionsAdmin"), helpText=msg.get("help.playerSubscriptionsAdmin"), icon="bi-person-check")

    @template.platform.adminPanel(content = @`
      <p class="text-body-secondary mb-3">${msg.get("playersub.counts", rowCount, replaceableCount)}</p>
      @template.platform.adminToolbar(content = @`
        <form method="get" action="${contextPath}/playersubscription-admin" class="d-flex align-items-center mb-0">
          @template.platform.seasonFilter(msg=msg, seasons=safeSeasons, seasonId=seasonId, selectId="playersub_season", autoSubmit=true, inline=true)
        </form>
      `)
    `)

    @template.platform.adminPanel(
      heading=msg.get("heading.pendingSubscriptionsAdmin"),
      subheading=msg.get("playersub.pendingCounts", pendingRowCount),
      content = @`
        <table class="table table-striped table-sm align-middle mb-0">
          <thead>
            <tr>
              <th scope="col">${msg.get("label.licence")}</th>
              <th scope="col">${msg.get("label.lastname")}</th>
              <th scope="col">${msg.get("label.firstname")}</th>
              <th scope="col">${msg.get("label.creationUser")}</th>
              <th scope="col">${msg.get("label.eventId")}</th>
              <th scope="col">${msg.get("label.event")}</th>
              <th scope="col">${msg.get("label.action")}</th>
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
                    @if(seasonId != null)
                      <input type="hidden" name="seasonId" value="${seasonId}">
                    @endif
                    <button type="submit" class="btn btn-sm btn-outline-danger">${msg.get("action.remove")}</button>
                  </form>
                </td>
              </tr>
            @else
              <tr>
                <td colspan="7" class="text-center text-body-secondary">${msg.get("playersub.noPendingSubscriptions")}</td>
              </tr>
            @endfor
          </tbody>
        </table>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("heading.orphanCustomPlayers"),
      subheading=msg.get("playersub.orphanCustomPlayers", orphanCount),
      content = @`
        <div class="d-flex justify-content-end mb-3">
          <form method="post" action="${contextPath}/playersubscription-admin" class="m-0">
            <input type="hidden" name="deleteOrphans" value="true">
            @if(seasonId != null)
              <input type="hidden" name="seasonId" value="${seasonId}">
            @endif
            @if(orphanCount > 0)
              <button type="submit" class="btn btn-outline-danger">${msg.get("action.deleteOrphanCustomPlayers")}</button>
            @else
              <button type="submit" class="btn btn-outline-danger" disabled>${msg.get("action.deleteOrphanCustomPlayers")}</button>
            @endif
          </form>
        </div>
        @if((orphanCustomPlayers != null && !orphanCustomPlayers.isEmpty()))
          <table class="table table-striped table-sm align-middle mb-0">
            <thead>
              <tr>
                <th scope="col">${msg.get("label.customPlayer")}</th>
                <th scope="col">${msg.get("label.customPlayerLicence")}</th>
                <th scope="col">${msg.get("label.creationUser")}</th>
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
        @else
          <p class="text-body-secondary mb-0">${msg.get("playersub.noOrphanCustomPlayers")}</p>
        @endif
      `
    )

    @template.platform.adminPanel(content = @`
      <div class="d-flex justify-content-end mb-3">
        <form method="post" action="${contextPath}/playersubscription-admin">
          <input type="hidden" name="all" value="true">
          @if(seasonId != null)
            <input type="hidden" name="seasonId" value="${seasonId}">
          @endif
          @if(replaceableCount > 0)
            <button type="submit" class="btn btn-primary">${msg.get("action.replaceAllByLicence")}</button>
          @else
            <button type="submit" class="btn btn-primary" disabled>${msg.get("action.replaceAllByLicence")}</button>
          @endif
        </form>
      </div>
      <table class="table table-striped table-sm align-middle mb-0">
        <thead>
          <tr>
            <th scope="col">${msg.get("label.subscription")}</th>
            <th scope="col">${msg.get("label.event")}</th>
            <th scope="col">${msg.get("label.creationUser")}</th>
            <th scope="col">${msg.get("label.currentReference")}</th>
            <th scope="col">${msg.get("label.customPlayer")}</th>
            <th scope="col">${msg.get("label.customPlayerLicence")}</th>
            <th scope="col">${msg.get("label.lucenePlayer")}</th>
            <th scope="col">${msg.get("label.replacementLicence")}</th>
            <th scope="col">${msg.get("label.status")}</th>
            <th scope="col">${msg.get("label.action")}</th>
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
                    @if(seasonId != null)
                      <input type="hidden" name="seasonId" value="${seasonId}">
                    @endif
                    <button type="submit" class="btn btn-sm btn-outline-primary">${msg.get("action.replaceByLicence")}</button>
                  </form>
                @endif
              </td>
            </tr>
          @else
            <tr>
              <td colspan="10" class="text-center text-body-secondary">${msg.get("info.noSubscription")}</td>
            </tr>
          @endfor
        </tbody>
      </table>
    `)
  `
)
