@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.MembershipOptionAccessRule[] accessRules
@param com.github.gcolin.membership.License license
@param com.github.gcolin.membership.MembershipOption option
@param com.github.gcolin.membership.LicensePrice price
!{i18ntitle = "Éditer Licence";}
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
                <li class="breadcrumb-item active" aria-current="page">Éditer</li>
            </ol>
        </nav>
        <div class="mb-3">
            <h1>@if(license.getId() != null)Éditer Licence@elseNouvelle Licence@endif</h1>
        </div>
        <div class="card">
            <div class="card-body">
                <form method="post" action="${contextPath}/license-admin">
                    @if(license.getId() != null)
                    <input type="hidden" name="id" value="${license.getId()}">
                    <div class="mb-3">
                        <label for="name" class="form-label">Nom</label>
                        <input type="text" class="form-control" id="name" name="name" value="${license.getName()}" required>
                    </div>
                    <div class="mb-3">
                        <label for="accessRule" class="form-label">Accessibilité</label>
                        <select class="form-select" id="accessRule" name="accessRule" required>
                            @for(var accessRule : accessRules)
                                @if(license.getAccessRule() != null && license.getAccessRule() == accessRule)
                                <option value="${accessRule.name()}" selected>
                                    @if("ALL".equals(accessRule.name()))Tous@endif
                                    @if("YOUNG_ONLY".equals(accessRule.name()))Young uniquement@endif
                                    @if("NON_YOUNG_ONLY".equals(accessRule.name()))Non young uniquement@endif
                                    @if("ADMIN".equals(accessRule.name()))Admin uniquement@endif
                                </option>
                                @else
                                <option value="${accessRule.name()}">
                                    @if("ALL".equals(accessRule.name()))Tous@endif
                                    @if("YOUNG_ONLY".equals(accessRule.name()))Young uniquement@endif
                                    @if("NON_YOUNG_ONLY".equals(accessRule.name()))Non young uniquement@endif
                                    @if("ADMIN".equals(accessRule.name()))Admin uniquement@endif
                                </option>
                                @endif
                            @endfor
                        </select>
                    </div>
                    <div class="mb-3">
                        <a href="${contextPath}/license-admin/${license.getId()}/price" class="btn btn-info">Gérer les tarifs</a>
                    </div>
                    <div class="mb-3">
                        <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
                        <button type="button" class="btn btn-danger" onclick="removeLicense(${license.getId()})">Supprimer</button>
                        <a href="${contextPath}/license-admin" class="btn btn-secondary">${msg.get("action.cancel")}</a>
                    </div>
                    @else
                    <div class="mb-3">
                        <label for="name" class="form-label">Nom</label>
                        <input type="text" class="form-control" id="name" name="name" value="" required>
                    </div>
                    <div class="mb-3">
                        <label for="accessRule" class="form-label">Accessibilité</label>
                        <select class="form-select" id="accessRule" name="accessRule" required>
                            @for(var accessRule : accessRules)
                                <option value="${accessRule.name()}">
                                    @if("ALL".equals(accessRule.name()))Tous@endif
                                    @if("YOUNG_ONLY".equals(accessRule.name()))Young uniquement@endif
                                    @if("NON_YOUNG_ONLY".equals(accessRule.name()))Non young uniquement@endif
                                    @if("ADMIN".equals(accessRule.name()))Admin uniquement@endif
                                </option>
                            @endfor
                        </select>
                    </div>
                    <div class="mb-3">
                        <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
                        <a href="${contextPath}/license-admin" class="btn btn-secondary">${msg.get("action.cancel")}</a>
                    </div>
                    @endif
                </form>
            </div>
        </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    <script>
        function removeLicense(id) {
            if (confirm('Êtes-vous sûr de vouloir supprimer cette licence ?')) {
                const form = document.createElement('form');
                form.method = 'post';
                form.action = '${contextPath}/license-admin';
                form.innerHTML = '<input type="hidden" name="id" value="' + id + '">' +
                                '<input type="hidden" name="toRemove" value="true">';
                document.body.appendChild(form);
                form.submit();
            }
        }
    </script>
</body>

</html>
