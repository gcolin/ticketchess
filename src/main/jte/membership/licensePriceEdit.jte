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
!{i18ntitle = "Éditer Tarif Licence";}
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
                <li class="breadcrumb-item"><a href="${contextPath}/license-admin/${license.getId()}/price">Tarifs ${license.getName()}</a></li>
                <li class="breadcrumb-item active" aria-current="page">@if(price.getId() != null)Éditer@elseNouveau@endif</li>
            </ol>
        </nav>
        <div class="mb-3">
            <h1>@if(price.getId() != null)Éditer Tarif@elseNouveau Tarif@endif</h1>
        </div>
        <div class="card">
            <div class="card-body">
                <form method="post" action="${contextPath}/license-admin/price/save">
                    <input type="hidden" name="licenseId" value="${license.getId()}">
                    @if(price.getId() != null)
                    <input type="hidden" name="id" value="${price.getId()}">
                    @endif
                    <div class="mb-3">
                        <label for="category" class="form-label">Catégorie</label>
                        <input type="text" class="form-control" id="category" name="category" value="${price.getCategory() != null ? price.getCategory() : ""}" required>
                    </div>
                    <div class="mb-3">
                        <label for="priceCents" class="form-label">Prix (€)</label>
                        <input type="number" class="form-control" id="priceCents" name="priceCents" step="0.01" value="@if(price.getPriceCents() != null)${String.format(java.util.Locale.US, "%.2f", price.getPriceCents() / 100.0)}@endif" required>
                    </div>
                    <small class="form-text text-muted">Le prix sera converti en centimes pour le stockage.</small>
                    <div class="mb-3">
                        <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
                        @if(price.getId() != null)
                        <button type="button" class="btn btn-danger" onclick="removePrice(${price.getId()}, ${license.getId()})">Supprimer</button>
                        @endif
                        <a href="${contextPath}/license-admin/${license.getId()}/price" class="btn btn-secondary">${msg.get("action.cancel")}</a>
                    </div>
                </form>
            </div>
        </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    <script>
        function removePrice(priceId, licenseId) {
            if (confirm('Êtes-vous sûr de vouloir supprimer ce tarif ?')) {
                const form = document.createElement('form');
                form.method = 'post';
                form.action = '${contextPath}/license-admin/price/save';
                form.innerHTML = '<input type="hidden" name="id" value="' + priceId + '">' +
                                '<input type="hidden" name="licenseId" value="' + licenseId + '">' +
                                '<input type="hidden" name="toRemove" value="true">';
                document.body.appendChild(form);
                form.submit();
            }
        }
    </script>
</body>

</html>
