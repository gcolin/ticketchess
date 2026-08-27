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
@param Object club
@param com.github.gcolin.event.Event event
@param java.util.List<com.github.gcolin.player.DisplayPlayer> favoritePlayers
@param Object p
@param com.github.gcolin.player.DisplayPlayer player
@param String query
@param java.util.List<com.github.gcolin.player.DisplayPlayer> players
@param Boolean registrationClosed
@param String registrationClosedMessageKey
    !{i18ntitle = msg.get("action.register");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

    <html>


    <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "register";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
        <main class="container flex-fill">
          <div class="bg-body p-3 pb-1 rounded mb-3">
            <h2>${msg.get("action.register")} ${event.getName()}</h2>
            @if(registrationClosed)
              <div class="alert alert-warning" role="alert">${msg.get(registrationClosedMessageKey)}</div>
              <div class="alert alert-info" role="alert">${msg.get("event.pendingQueueAvailable")}</div>
            @endif
          <form method="GET" class="mb-3">
            <div class="mb-3">
              <label for="player" class="form-label">${msg.get("search.label")}</label>
              <input type="text" class="form-control" id="player" name="query"  value="${query != null ? query : ""}"/>
            </div>
            <div class="d-flex align-items-center">
                <button type="submit" id="searchplayer" class="btn btn-primary">
                  ${msg.get("action.search")}
                </button>

                <a href="${contextPath}/customplayer/new?eventId=${event.getId()}"
                   class="ms-3 btn btn-secondary"
                   id="createPlayer">
                   ${msg.get("action.create")}
                </a>
              </div>
          </form>
          </div>
              @if(players != null)
           @if(players.size() > 0)
          <div class="rounded mb-3 " style="overflow: hidden;"> 
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th scope="col">${msg.get("player.licence")}</th>
                <th scope="col">${msg.get("player.name")}</th>
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.category")}</th>
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.rating")}</th>
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.club")}</th>
                @if(!event.isFree())
                <th scope="col">${msg.get("event.price")}</th>
                @endif
                <th scope="col"></th>
              </tr>
            </thead>
            <tbody>
                @for(var pl : players)
              <tr>
                <td>${pl.getLicence() != null ? pl.getLicence() : ""}</td>
                <td>${pl.getName()} ${pl.getFirstname()} @if(pl.getFideTitre() != null)<span class="badge bg-success">${msg.get("fidetitre." + pl.getFideTitre())}</span>@endif</td>
                <td class="d-none d-md-table-cell">${pl.getCategory() != null ? pl.getCategory() : ""}</td>
                <td class="d-none d-md-table-cell">${pl.getRating() != null ? pl.getRating() : ""}</td>
                <td class="d-none d-md-table-cell">${pl.getClub() != null ? pl.getClub() : ""}</td>
                @if(!event.isFree())
                <td>${pl.getPrice()}</td>
                @endif
                <td><form method="POST">
                  <input type="hidden" name="nrffe" value="${pl.getLicence()}"/>
                  <button type="submit" class="btn-sm btn btn-secondary js-select">&#10133; ${msg.get(registrationClosed != null && registrationClosed ? "action.joinPendingQueue" : "action.select")}</button></form></td>
                </tr>
              @endfor
            </tbody>
          </table>
          </div>

          @elseif(players.size() == 0)
            <div class="bg-body p-3 pb-1 rounded mb-3">
          	<p><span>${msg.get("info.noplayer")}</span></p></div>
          @endif
          @endif
          @if(favoritePlayers != null && favoritePlayers.size() > 0)
          <div class="bg-body p-3 pb-1 rounded mb-3">
            <h3>${msg.get("search.favorites")}</h3>
            <div class="rounded mb-3 " style="overflow: hidden;"> 
            <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th scope="col">${msg.get("player.licence")}</th>
                <th scope="col">${msg.get("player.name")}</th>
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.category")}</th>
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.rating")}</th>
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.club")}</th>
                @if(!event.isFree())
                  <th scope="col">${msg.get("event.price")}</th>
                @endif
                <th scope="col"></th>
              </tr>
            </thead>
            <tbody>
                @for(var favPl : favoritePlayers)
              <tr>
                <td>${favPl.getLicence() != null ? favPl.getLicence() : ""}</td>
                <td>${favPl.getName()} ${favPl.getFirstname()}</td>
                <td class="d-none d-md-table-cell">${favPl.getCategory()}</td>
                <td class="d-none d-md-table-cell">${favPl.getRating()}</td>
                <td class="d-none d-md-table-cell">${favPl.getClub() != null ? favPl.getClub() : ""}</td>
                @if(!event.isFree())
                  <td>${favPl.getPrice()}</td>
                @endif
                <td><form method="POST">
                  <input type="hidden" name="id" value="${event.getId()}"/>
                  <input type="hidden" name="nrffe" value="${favPl.getNrffeId()}"/>
                  <button type="submit" class="btn-sm btn btn-secondary">&#10133; ${msg.get(registrationClosed != null && registrationClosed ? "action.joinPendingQueue" : "action.select")}</button></form></td>
                </tr>
              @endfor
            </tbody>
          </table>
          </div>
          </div>
          @endif
        </main>
		@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    </body>

    </html>