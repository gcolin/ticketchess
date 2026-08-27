@param String contextPath
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.platform.StateService state
@param com.github.gcolin.notification.Notifications notifications
@param com.github.gcolin.platform.Page pageConfig
@param String page
@param com.github.gcolin.platform.Messages msg
<nav class="navbar navbar-expand-md navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand d-flex align-items-center" href="${contextPath}/">
            @if(pageConfig.getLogoUrl() != null && !pageConfig.getLogoUrl().isBlank())
            <img src="${contextPath}${pageConfig.getLogoUrl()}" height="30" class="me-2" alt="">
            @endif
            @if(user.isLogged())
                <span class="d-md-none"><i class="bi bi-person-circle"></i> ${user.getUsername()}</span><span class="d-none d-md-block">${pageConfig.getTitle()}</span>
            @else
                <span>${pageConfig.getTitle()}</span>
            @endif
        </a>
        <button class="navbar-toggler"
            type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse"
            aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarCollapse">
            <ul class="navbar-nav me-auto mb-2 mb-md-0">
                <li class="nav-item">
                    <a id="events" class="nav-link ${"events".equals(page) ? "active" : ""}" aria-current="page" href="${contextPath}/event">${msg.get("menu.events")}</a>
                </li>
                @if(user.isLogged())
                <li class="nav-item">
                    <a id="myevents" class="nav-link ${"myevents".equals(page) ? "active" : ""}" href="${contextPath}/event/my">${msg.get("menu.myevents")}
                        @if(user.getDebt() > 0)
                            <span class="badge bg-warning text-dark">${String.format(java.util.Locale.US, "%.2f", user.getDebt())} ${msg.get("info.topaid")}</span>
                        @endif
                    </a>
                </li>
                @if(user.canSeeAdminMenu())
                    <ul class="navbar-nav">
                        <li class="nav-item">
                            <a id="admin" class="nav-link ${"admin".equals(page) ? "active" : ""}" href="${contextPath}/admin">
                                ${msg.get("menu.admin")}
                            </a>
                        </li>
                    </ul>
                @endif
                @endif
            </ul>
            @if(user.isLogged())
                <ul class="navbar-nav">
                    @if(pageConfig.isClubRegisterEnabled())
                    <li class="nav-item me-2">
                        <a id="registerNav" class="nav-link ${"clubregister".equals(page) ? "active" : ""}" href="${contextPath}/club-register">
                            ${msg.get("action.registerClub")}
                        </a>
                    </li>
                    @endif
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="bi bi-person-circle"></i> ${user.getUsername()}
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="navbarDropdown">
                            @if(pageConfig.getAccountUrl() != null && !pageConfig.getAccountUrl().isBlank())
                            <li><a id="account" class="dropdown-item" href="${pageConfig.getAccountUrl()}">${msg.get("menu.account")}</a></li>
                            @endif
                            <li><a id="logout" class="dropdown-item" href="${contextPath}/logout">${msg.get("menu.disconnect")}</a></li>
                        </ul>
                    </li>
                </ul>
            @else
                <ul class="navbar-nav d-flex">
                    @if(pageConfig.isClubRegisterEnabled())
                    <li class="nav-item me-2">
                        <a id="registerNav" class="nav-link ${"clubregister".equals(page) ? "active" : ""}" href="${contextPath}/club-register">
                            ${msg.get("action.registerClub")}
                        </a>
                    </li>
                    @endif
                    <li class="nav-item">
                    <a id="googleauth" href="${state.getLogin()}" class="btn btn-outline-light">${msg.get("action.login")}</a>
                    </li>
                </ul>
            @endif
        </div>
    </div>
</nav>

<div class="mb-4">
@if(notifications.getGlobal() != null)
@for(var notif : notifications.getGlobal())
<div class="alert alert-primary pb-0 rounded-0 mb-0" role="alert">
    ${notif.getContent()}
</div>
@endfor
@endif
</div>
