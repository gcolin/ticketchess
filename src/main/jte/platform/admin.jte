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
@param Object collection
@param com.github.gcolin.event.Event event
@param java.util.List<com.github.gcolin.event.EventCollection> eventCollections
@param com.github.gcolin.event.EventGroup eventGroup
@param com.github.gcolin.membership.License license
@param com.github.gcolin.membership.Membership membership
@param java.util.List<com.github.gcolin.membership.Membership> memberships
@param com.github.gcolin.membership.MembershipOption option
@param java.util.List<com.github.gcolin.membership.MembershipOption> options
@param Object p
@param com.github.gcolin.payment.Payment payment
@param java.util.List<com.github.gcolin.payment.Payment> payments
@param java.util.List<com.github.gcolin.player.Player> players
@param Object statistics
@param java.util.List<String> users
!{i18ntitle = msg.get("page.admin");}
!{title = i18ntitle;}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  adminHub=true,
  alerts = @`
    @if("cacheCleared".equals(success))
      <div class="alert alert-success mb-3" role="alert">${msg.get("admin.cacheCleared")}</div>
    @endif
  `,
  content = @`
    @template.platform.adminPageHeader(title=msg.get("page.admin"), helpText=msg.get("help.adminHub"))

    @if(user.hasPermission("EVENT_CREATE") || user.hasPermission("EVENT_EDIT"))
      @template.platform.adminPanel(
        heading=msg.get("admin.section.event"),
        content = @`
        <div class="row g-3">
          @if(user.hasPermission("EVENT_EDIT"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/admin/events">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-list-nested fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.adminEvents")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.adminEventsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/playersubscription-admin">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-person-check fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.playerSubscriptionsAdmin")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.playerSubscriptionsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
          @if(user.canManagePayments())
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/payment">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-credit-card fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.payments")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.paymentsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif

          <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/dashboard">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-speedometer2 fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.dashboard")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.dashboardDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/statistics">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-bar-chart fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.statistics")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.statisticsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
        </div>
        `
      )
    @endif

    @if(user.hasPermission("EVENT_EDIT") || user.hasPermission("ADMIN_PANEL") || user.hasPermission("USER_IMPERSONATE"))
      @template.platform.adminPanel(
        heading=msg.get("admin.section.users"),
        content = @`
        <div class="row g-3">
          @if(user.hasPermission("EVENT_EDIT"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/customplayer">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-people fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.players")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.playersDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
          @if(user.hasPermission("ADMIN_PANEL"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/database/manual-players">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-person-plus fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.databasePlayers")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.databasePlayersDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/database">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-database fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.database")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.databaseDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/logged-users">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-person-lines-fill fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.loggedUsers")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.loggedUsersDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
          @if(user.hasPermission("USER_IMPERSONATE"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/logas">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-incognito fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.logas")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.logasDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
          @if(user.hasPermission("ADMIN_PANEL"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/user-authorization">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-shield-lock fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.userAuthorizations")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.userAuthorizationsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
        </div>
        `
      )
    @endif

    @if(user.hasPermission("EVENT_EDIT") || user.hasPermission("ADMIN_PANEL"))
      @template.platform.adminPanel(
        heading=msg.get("admin.section.membership"),
        content = @`
        <div class="row g-3">
          @if(user.hasPermission("EVENT_EDIT"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/membership">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-person-vcard fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.memberships")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.membershipsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
          @if(user.hasPermission("ADMIN_PANEL"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/membership-option-admin">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-toggles fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.membershipOptions")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.membershipOptionsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/license-admin">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-card-list fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("menu.licenses")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.licensesDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
        </div>
        `
      )
    @endif

    @if(user.hasPermission("ADMIN_PANEL") || user.hasPermission("MAIL_SEND"))
      @template.platform.adminPanel(
        heading=msg.get("admin.section.system"),
        content = @`
        <div class="row g-3">
          @if(user.hasPermission("ADMIN_PANEL"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/admin/org">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-bank fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("page.adminOrg")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.ribDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/admin/seasons">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-calendar-range fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("page.seasons")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.seasonsDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/admin/clear-cache">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-arrow-clockwise fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("action.clearCache")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.clearCacheDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
          @if(user.hasPermission("MAIL_SEND"))
            <div class="col-12 col-md-6 col-xl-4">
              <a class="card h-100 text-decoration-none" href="${contextPath}/admin/mail">
                <div class="card-body">
                  <div class="d-flex align-items-start gap-3">
                    <i class="bi bi-envelope fs-3 text-primary"></i>
                    <div>
                      <h3 class="h5 mb-1">${msg.get("page.adminMail")}</h3>
                      <p class="text-body-secondary mb-0">${msg.get("admin.tile.mailDesc")}</p>
                    </div>
                  </div>
                </div>
              </a>
            </div>
          @endif
        </div>
        `
      )
    @endif
  `
)
