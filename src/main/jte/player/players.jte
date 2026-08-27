@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<com.github.gcolin.player.CustomPlayer> players
@param int pageSize
@param long totalItems
@param int currentPage
@param int totalPages
@param boolean hasPrev
@param boolean hasNext
@param int prevPage
@param int nextPage
@param int startIndex
!{i18ntitle = msg.get("page.players");}
!{title = i18ntitle;}
!{if (players == null) players = java.util.List.of();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

    <html>


    <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "event";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
        <main class="container flex-fill">

          <nav aria-label="breadcrumb" class="mb-3 mt-3">
            <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
              <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
              <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
              <li class="breadcrumb-item active" aria-current="page">${msg.get("page.players")}</li>
            </ol>
          </nav>

          <div class="bg-body p-3 mb-3">
            <h2>${msg.get("page.players")}</h2>
          </div>

          <div class="d-flex justify-content-between align-items-center mb-3 bg-body rounded px-3 py-2">
            <div class="text-body-secondary">
              ${msg.get("pagination.playerCount", totalItems, currentPage, totalPages)}
            </div>
            <div class="d-flex align-items-center gap-2">
              <select class="form-select form-select-sm" style="width:auto" onchange="window.location='${contextPath}/customplayer?page=1&size='+this.value">
                @for(var s : java.util.List.of(10, 25, 50, 100))
                  @if(pageSize == s)
                  <option value="${s}" selected>${s}</option>
                  @else
                  <option value="${s}">${s}</option>
                  @endif
                @endfor
              </select>
              @if(hasPrev)
                <a class="btn btn-outline-dark btn-sm" href="${contextPath}/customplayer?page=${prevPage}&size=${pageSize}">${msg.get("pagination.previous")}</a>
              @else
                <span class="btn btn-outline-tertiary btn-sm disabled">${msg.get("pagination.previous")}</span>
              @endif
              @if(hasNext)
                <a class="btn btn-outline-dark btn-sm" href="${contextPath}/customplayer?page=${nextPage}&size=${pageSize}">${msg.get("pagination.next")}</a>
              @else
                <span class="btn btn-outline-tertiary btn-sm disabled">${msg.get("pagination.next")}</span>
              @endif
            </div>
          </div>

          <div class="table-responsive">
            <table class="table table-striped">
              <thead>
                <tr>
                  <th scope="col"></th>
                  <th scope="col" class="d-none d-md-table-cell">${msg.get("player.licence")}</th>
                  <th scope="col">${msg.get("player.name")}</th>
                  <th scope="col" class="d-none d-md-table-cell">${msg.get("player.birthdate")}</th>
                  <th scope="col">${msg.get("player.rating")}</th>
                </tr>
              </thead>
              <tbody>
                @if(players.isEmpty())
                  <tr>
                    <td colspan="5" class="text-center text-muted">${msg.get("info.noPlayer")}</td>
                  </tr>
                @else
                @for(int i = 0; i < players.size(); i++)
                  <tr>
                    <td>${startIndex + i + 1}</td>
                    <td class="d-none d-md-table-cell"><a href="${contextPath}/customplayer/${String.valueOf(players.get(i).getId())}">${String.valueOf(players.get(i).getId())}</a></td>
                    <td><a href="${contextPath}/customplayer/${String.valueOf(players.get(i).getId())}">${players.get(i).getName()} ${players.get(i).getFirstname()}</a></td>
                    <td class="d-none d-md-table-cell">${players.get(i).getBirthDate() != null ? players.get(i).getBirthDate() : ""}</td>
                    <td>${players.get(i).getRating() != null ? players.get(i).getRating() : ""}</td>
                  </tr>
                @endfor
                @endif
              </tbody>
            </table>
          </div>


        </main>

         @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)

    </body>

    </html>
