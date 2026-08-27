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
@param com.github.gcolin.membership.LicensePrice price
@param java.util.List<com.github.gcolin.membership.LicensePrice> prices
!{i18ntitle = "Tarifs Licence";}
!{title = i18ntitle;}
!{page = "license";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE html>
<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
        <nav aria-label="breadcrumb" class="mb-3 mt-3">
            <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
                <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
                <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
                <li class="breadcrumb-item"><a href="${contextPath}/license-admin">Licences</a></li>
                <li class="breadcrumb-item active" aria-current="page">Tarifs pour ${license.getName()}</li>
            </ol>
        </nav>
        <div class="mb-3">
            <h1>Tarifs - Licence ${license.getName()}</h1>
        </div>
        <div>
            <a href="${contextPath}/license-admin/${license.getId()}/price/new" class="btn btn-primary mb-3">${msg.get("action.createGeneric")}</a>
            <a href="${contextPath}/license-admin" class="btn btn-secondary mb-3">${msg.get("action.cancel")}</a>
        </div>
        <table class="table table-striped">
            <thead>
                <tr>
                    <th scope="col">#</th>
                    <th scope="col">Catégorie</th>
                    <th scope="col">Prix</th>
                    <th scope="col">Actions</th>
                </tr>
            </thead>
            <tbody>
                @for(var lp : prices)
                <tr>
                    <td>${lp.getId()}</td>
                    <td>${lp.getCategory()}</td>
                    <td>${String.format(java.util.Locale.US, "%.2f", lp.getPriceCents() / 100.0)} €</td>
                    <td>
                        <a href="${contextPath}/license-admin/${license.getId()}/price/${lp.getId()}/edit" class="btn btn-sm btn-warning">Éditer</a>
                    </td>
                </tr>
                @endfor
            </tbody>
        </table>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>