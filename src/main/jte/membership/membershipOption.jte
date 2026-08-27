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
@param java.util.List<com.github.gcolin.membership.MembershipOption> options
!{i18ntitle = msg.get("page.membershipOptions");}
!{title = i18ntitle;}
!{page = "admin";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3 mt-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("breadcrumb.membershipOptions")}</li>
      </ol>
    </nav>

    <div class="mb-3">
      <a class="btn btn-success" href="${contextPath}/membership-option-admin/new">${msg.get("action.addMembershipOption")}</a>
    </div>

    <div class="table-responsive bg-body rounded p-2">
      <table class="table table-striped align-middle mb-0">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">Type</th>
            <th scope="col">Valeur</th>
            <th scope="col">Licence</th>
            <th scope="col">Montant (€)</th>
            <th scope="col">Accessibilite</th>
            <th scope="col">Créé</th>
            <th scope="col">Modifié</th>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          @for(var opt : options)
            <tr>
              <td>${opt.getId()}</td>
              <td>${opt.getOptionType() != null ? opt.getOptionType().name() : ""}</td>
              <td>${opt.getOptionValue() != null ? opt.getOptionValue() : ""}</td>
              <td>@if(opt.getLicense() != null)Licence ${opt.getLicense().getName()}@elseToutes@endif</td>
              <td>${(opt.getAmountCents() != null ? opt.getAmountCents() : 0) / 100.0} €</td>
              <td>
                @if(opt.getAccessRule() == null || "ALL".equals(opt.getAccessRule().name()))
                  Tous
                @elseif("YOUNG_ONLY".equals(opt.getAccessRule().name()))
                  Young uniquement
                @elseif("NON_YOUNG_ONLY".equals(opt.getAccessRule().name()))
                  Non young uniquement
                @elseif("ADMIN".equals(opt.getAccessRule().name()))
                  Admin uniquement
                @endif
              </td>
              <td>@if(opt.getCreatedAt() != null)${opt.getCreatedAt().toString()}@endif</td>
              <td>@if(opt.getUpdatedAt() != null)${opt.getUpdatedAt().toString()}@endif</td>
              <td>
                <div class="d-flex gap-2">
                <a class="btn btn-sm btn-outline-primary" href="${contextPath}/membership-option-admin/${opt.getId()}/edit">Modifier</a>
                <form method="post" action="${contextPath}/membership-option-admin/${opt.getId()}/delete" onsubmit="return confirm('Supprimer cette option ?')">
                  <button type="submit" class="btn btn-sm btn-outline-danger">Supprimer</button>
                </form>
                </div>
              </td>
            </tr>
          @else
            <tr><td colspan="9" class="text-center text-body-secondary">Aucune option configurée.</td></tr>
          @endfor
        </tbody>
      </table>
    </div>
  </main>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>
