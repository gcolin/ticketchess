@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.License license
@param java.util.List<com.github.gcolin.membership.License> licenses
@param com.github.gcolin.membership.LicensePrice price
@param Object type
<!DOCTYPE html>
!{i18ntitle = "Gestion des Licences";}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "license";}
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
        <nav aria-label="breadcrumb" class="mb-3 mt-3">
            <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
                <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
                <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
                <li class="breadcrumb-item active" aria-current="page">Licences</li>
            </ol>
        </nav>
        <div class="mb-3">
            <h1>Gestion des Licences</h1>
        </div>
        <div>
            <form method="post" action="${contextPath}/license-admin" class="d-inline-block">
                <input type="hidden" name="id" value="">
                <input type="hidden" name="name" value="">
                <button type="button" class="btn btn-primary mb-3" onclick="addLicense()">${msg.get("action.createGeneric")}</button>
            </form>
        </div>
        <table class="table table-striped">
            <thead>
                <tr>
                    <th scope="col">#</th>
                    <th scope="col">Nom</th>
                    <th scope="col">Actions</th>
                </tr>
            </thead>
            <tbody>
                @for(var lic : licenses)
                <tr>
                    <td>${lic.getId()}</td>
                    <td>${lic.getName()}</td>
                    <td>
                        <a href="${contextPath}/license-admin/${lic.getId()}" class="btn btn-sm btn-warning">Éditer</a>
                        <a href="${contextPath}/license-admin/${lic.getId()}/price" class="btn btn-sm btn-info">Tarifs</a>
                    </td>
                </tr>
                @endfor
            </tbody>
        </table>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>