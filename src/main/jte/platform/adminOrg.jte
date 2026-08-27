@param String contextPath
@param String error
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param boolean ribAvailable
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param Object club
@param Integer deleteFailed
@param com.github.gcolin.event.Event event
@param com.github.gcolin.membership.Membership membership
@param com.github.gcolin.notification.Notification notif
@param com.github.gcolin.membership.MembershipOption option
@param Object p
@param com.github.gcolin.payment.Payment payment
@param String status
@param Object type
@param String tab
@param java.util.Map<String, String> cfg
@param java.util.Map<String, Boolean> secretsConfigured
@param boolean clubRegisterEnabled
@param boolean logoAvailable
@param boolean backgroundAvailable
@param String ribSize
!{i18ntitle = msg.get("page.adminOrg");}
!{title = i18ntitle;}
!{page = "admin";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.adminOrg")}</li>
        </ol>
      </nav>

      @if("ribUploaded".equals(success))
        <div class="alert alert-success mb-3" role="alert">${msg.get("admin.ribUploaded")}</div>
      @elseif("logoUploaded".equals(success))
        <div class="alert alert-success mb-3" role="alert">${msg.get("admin.logoUploaded")}</div>
      @elseif("backgroundUploaded".equals(success))
        <div class="alert alert-success mb-3" role="alert">${msg.get("admin.backgroundUploaded")}</div>
      @elseif("logoDeleted".equals(success))
        <div class="alert alert-success mb-3" role="alert">${msg.get("admin.logoDeleted")}</div>
      @elseif("backgroundDeleted".equals(success))
        <div class="alert alert-success mb-3" role="alert">${msg.get("admin.backgroundDeleted")}</div>
      @elseif("ribDeleted".equals(success))
        <div class="alert alert-success mb-3" role="alert">${msg.get("admin.ribDeleted")}</div>
      @elseif("configSaved".equals(success))
        <div class="alert alert-success mb-3" role="alert">${msg.get("admin.configSaved")}</div>
      @endif
      @if("ribUploadFailed".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.ribUploadFailed")}</div>
      @elseif("invalidRib".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.ribInvalid")}</div>
      @elseif("ribTooLarge".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.ribTooLarge")}</div>
      @elseif("logoUploadFailed".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.logoUploadFailed")}</div>
      @elseif("invalidLogo".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.logoInvalid")}</div>
      @elseif("logoTooLarge".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.logoTooLarge")}</div>
      @elseif("backgroundUploadFailed".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.backgroundUploadFailed")}</div>
      @elseif("invalidBackground".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.backgroundInvalid")}</div>
      @elseif("backgroundTooLarge".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.backgroundTooLarge")}</div>
      @elseif("logoDeleteFailed".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.logoDeleteFailed")}</div>
      @elseif("backgroundDeleteFailed".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.backgroundDeleteFailed")}</div>
      @elseif("ribDeleteFailed".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.ribDeleteFailed")}</div>
      @elseif("configSaveFailed".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.configSaveFailed")}</div>
      @elseif("configInvalid".equals(error))
        <div class="alert alert-danger mb-3" role="alert">${msg.get("admin.configInvalid")}</div>
      @endif
      <div id="rib-status" class="alert d-none mb-3" role="alert"></div>

      <div class="card mb-4">
        <div class="card-body">
          <h1 class="mb-2">${msg.get("page.adminOrg")}</h1>
          <p class="text-body-secondary mb-0">${msg.get("help.adminOrg")}</p>
        </div>
      </div>

      <ul class="nav nav-tabs bg-white rounded-top-2" id="org-tabs" role="tablist">
        <li class="nav-item" role="presentation">
          <button class="nav-link ${("club".equals(tab) ? "active" : "")}" id="club-tab" data-bs-toggle="tab" data-bs-target="#club" type="button" role="tab">${msg.get("admin.tab.club")}</button>
        </li>
        <li class="nav-item" role="presentation">
          <button class="nav-link ${("invoice".equals(tab) ? "active" : "")}" id="invoice-tab" data-bs-toggle="tab" data-bs-target="#invoice" type="button" role="tab">${msg.get("admin.tab.invoice")}</button>
        </li>
        <li class="nav-item" role="presentation">
          <button class="nav-link ${("files".equals(tab) ? "active" : "")}" id="files-tab" data-bs-toggle="tab" data-bs-target="#files" type="button" role="tab">${msg.get("admin.tab.files")}</button>
        </li>
        <li class="nav-item" role="presentation">
          <button class="nav-link ${("stripe".equals(tab) ? "active" : "")}" id="stripe-tab" data-bs-toggle="tab" data-bs-target="#stripe" type="button" role="tab">${msg.get("admin.tab.stripe")}</button>
        </li>
        <li class="nav-item" role="presentation">
          <button class="nav-link ${("oauth".equals(tab) ? "active" : "")}" id="oauth-tab" data-bs-toggle="tab" data-bs-target="#oauth" type="button" role="tab">${msg.get("admin.tab.oauth")}</button>
        </li>
        <li class="nav-item" role="presentation">
          <button class="nav-link ${("system".equals(tab) ? "active" : "")}" id="system-tab" data-bs-toggle="tab" data-bs-target="#system" type="button" role="tab">${msg.get("admin.tab.system")}</button>
        </li>
      </ul>

      <div class="tab-content bg-body rounded-bottom p-3 mb-4 border border-top-0">
        <div class="tab-pane fade ${("club".equals(tab) ? "show active" : "")}" id="club" role="tabpanel">
          <form method="post" action="${contextPath}/admin/org/config">
            <input type="hidden" name="tab" value="club">
            <div class="mb-3"><label class="form-label" for="title">${msg.get("admin.field.title")}</label>
              <input class="form-control" id="title" name="title" value="${cfg.get("title") != null ? cfg.get("title") : ""}"></div>
            <div class="mb-3"><label class="form-label" for="contact.url">${msg.get("admin.field.contactUrl")}</label>
              <input class="form-control" id="contact.url" name="contact.url" value="${cfg.get("contact.url") != null ? cfg.get("contact.url") : ""}"></div>
            <div class="mb-3"><label class="form-label" for="source.url">${msg.get("admin.field.sourceUrl")}</label>
              <input class="form-control" id="source.url" name="source.url" type="url" value="${cfg.get("source.url") != null ? cfg.get("source.url") : ""}" placeholder="https://github.com/owner/ticketchess">
              <div class="form-text">${msg.get("help.adminField.sourceUrl")}</div></div>
            <div class="mb-3"><label class="form-label" for="org.name">${msg.get("admin.field.orgName")}</label>
              <input class="form-control" id="org.name" name="org.name" value="${cfg.get("org.name") != null ? cfg.get("org.name") : ""}"></div>
            <div class="mb-3"><label class="form-label" for="org.email">${msg.get("admin.field.orgEmail")}</label>
              <input class="form-control" id="org.email" name="org.email" type="email" value="${cfg.get("org.email") != null ? cfg.get("org.email") : ""}"></div>
            <div class="mb-3"><label class="form-label" for="org.address">${msg.get("admin.field.orgAddress")}</label>
              <input class="form-control" id="org.address" name="org.address" value="${cfg.get("org.address") != null ? cfg.get("org.address") : ""}"></div>
            <div class="mb-3"><label class="form-label" for="org.hosting.address">${msg.get("admin.field.orgHosting")}</label>
              <input class="form-control" id="org.hosting.address" name="org.hosting.address" value="${cfg.get("org.hosting.address") != null ? cfg.get("org.hosting.address") : ""}"></div>
            <div class="mb-3">
              <label class="form-label" for="membership.notif.emails">${msg.get("admin.field.membershipNotifEmails")}</label>
              @if(clubRegisterEnabled)<span class="badge text-bg-success mb-1">${msg.get("admin.clubRegister.enabled")}</span>@else<span class="badge text-bg-warning mb-1">${msg.get("admin.clubRegister.disabled")}</span>@endif
              <input class="form-control" id="membership.notif.emails" name="membership.notif.emails" value="${cfg.get("membership.notif.emails") != null ? cfg.get("membership.notif.emails") : ""}" placeholder="admin@club.fr, tresorier@club.fr">
              <div class="form-text">${msg.get("help.adminField.membershipNotifEmails")}</div>
            </div>
            <button class="btn btn-primary" type="submit">${msg.get("action.save")}</button>
          </form>
        </div>

        <div class="tab-pane fade ${("invoice".equals(tab) ? "show active" : "")}" id="invoice" role="tabpanel">
          <form method="post" action="${contextPath}/admin/org/config">
            <input type="hidden" name="tab" value="invoice">
            <div class="row">
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerName")}</label>
                <input class="form-control" name="invoice.seller.name" value="${cfg.get("invoice.seller.name") != null ? cfg.get("invoice.seller.name") : ""}"></div>
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerEmail")}</label>
                <input class="form-control" name="invoice.seller.email" value="${cfg.get("invoice.seller.email") != null ? cfg.get("invoice.seller.email") : ""}"></div>
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerAddress1")}</label>
                <input class="form-control" name="invoice.seller.address1" value="${cfg.get("invoice.seller.address1") != null ? cfg.get("invoice.seller.address1") : ""}"></div>
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerAddress2")}</label>
                <input class="form-control" name="invoice.seller.address2" value="${cfg.get("invoice.seller.address2") != null ? cfg.get("invoice.seller.address2") : ""}"></div>
              <div class="col-md-3 mb-3"><label class="form-label">${msg.get("admin.field.sellerZip")}</label>
                <input class="form-control" name="invoice.seller.zip" value="${cfg.get("invoice.seller.zip") != null ? cfg.get("invoice.seller.zip") : ""}"></div>
              <div class="col-md-5 mb-3"><label class="form-label">${msg.get("admin.field.sellerCity")}</label>
                <input class="form-control" name="invoice.seller.city" value="${cfg.get("invoice.seller.city") != null ? cfg.get("invoice.seller.city") : ""}"></div>
              <div class="col-md-4 mb-3"><label class="form-label">${msg.get("admin.field.sellerCountry")}</label>
                <input class="form-control" name="invoice.seller.country" value="${cfg.get("invoice.seller.country") != null ? cfg.get("invoice.seller.country") : ""}"></div>
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerPhone")}</label>
                <input class="form-control" name="invoice.seller.phone" value="${cfg.get("invoice.seller.phone") != null ? cfg.get("invoice.seller.phone") : ""}"></div>
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerWebsite")}</label>
                <input class="form-control" name="invoice.seller.website" value="${cfg.get("invoice.seller.website") != null ? cfg.get("invoice.seller.website") : ""}"></div>
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerSiret")}</label>
                <input class="form-control" name="invoice.seller.siret" value="${cfg.get("invoice.seller.siret") != null ? cfg.get("invoice.seller.siret") : ""}"></div>
              <div class="col-md-6 mb-3"><label class="form-label">${msg.get("admin.field.sellerRna")}</label>
                <input class="form-control" name="invoice.seller.rna" value="${cfg.get("invoice.seller.rna") != null ? cfg.get("invoice.seller.rna") : ""}"></div>
              <div class="col-12 mb-3"><label class="form-label">${msg.get("admin.field.sellerPrefecture")}</label>
                <input class="form-control" name="invoice.seller.prefecture" value="${cfg.get("invoice.seller.prefecture") != null ? cfg.get("invoice.seller.prefecture") : ""}"></div>
              <div class="col-md-4 mb-3"><label class="form-label">${msg.get("admin.field.invoicePrefix")}</label>
                <input class="form-control" name="invoice.number.prefix" value="${cfg.get("invoice.number.prefix") != null ? cfg.get("invoice.number.prefix") : ""}"></div>
              <div class="col-md-8 mb-3"><label class="form-label">${msg.get("admin.field.invoiceFooter")}</label>
                <input class="form-control" name="invoice.footer" value="${cfg.get("invoice.footer") != null ? cfg.get("invoice.footer") : ""}"></div>
              <div class="col-12 mb-3"><label class="form-label">${msg.get("admin.field.invoiceVat")}</label>
                <input class="form-control" name="invoice.vat.notice" value="${cfg.get("invoice.vat.notice") != null ? cfg.get("invoice.vat.notice") : ""}"></div>
            </div>
            <button class="btn btn-primary" type="submit">${msg.get("action.save")}</button>
          </form>
        </div>

        <div class="tab-pane fade ${("files".equals(tab) ? "show active" : "")}" id="files" role="tabpanel">
          <div class="row g-4">
            <div class="col-md-4">
              <h2 class="h5 mb-3">${msg.get("admin.logo.current")}</h2>
              @if(logoAvailable)
                <img src="${contextPath}/logo" alt="" class="img-fluid mb-3 border rounded" style="max-height: 80px;">
                <div class="mb-3">
                  <button class="btn btn-outline-danger" type="button" id="logo-delete-btn">${msg.get("action.remove")}</button>
                </div>
              @else
                <p class="text-body-secondary">${msg.get("admin.logo.missing")}</p>
              @endif
              <form id="logo-upload-form">
                <div class="mb-3">
                  <label class="form-label" for="logo-file">${msg.get("admin.logo.fileLabel")}</label>
                  <input class="form-control" type="file" id="logo-file" accept="image/png,image/jpeg,image/webp,.png,.jpg,.jpeg,.webp" required>
                </div>
                <button class="btn btn-primary" type="submit" id="logo-upload-btn">${msg.get("admin.logo.upload")}</button>
              </form>
            </div>
            <div class="col-md-4">
              <h2 class="h5 mb-3">${msg.get("admin.background.current")}</h2>
              @if(backgroundAvailable)
                <div class="mb-3 border rounded overflow-hidden" style="height: 80px; background: url('${contextPath}/background') center / cover no-repeat;"></div>
                <div class="mb-3">
                  <button class="btn btn-outline-danger" type="button" id="background-delete-btn">${msg.get("action.remove")}</button>
                </div>
              @else
                <p class="text-body-secondary mb-3">${msg.get("admin.background.missing")}</p>
                <div class="mb-3 border rounded overflow-hidden" style="height: 80px; background: url('${contextPath}/assets/6.jpg') center / cover no-repeat;"></div>
              @endif
              <form id="background-upload-form">
                <div class="mb-3">
                  <label class="form-label" for="background-file">${msg.get("admin.background.fileLabel")}</label>
                  <input class="form-control" type="file" id="background-file" accept="image/png,image/jpeg,image/webp,.png,.jpg,.jpeg,.webp" required>
                </div>
                <button class="btn btn-primary" type="submit" id="background-upload-btn">${msg.get("admin.background.upload")}</button>
              </form>
            </div>
            <div class="col-md-4">
              <h2 class="h5 mb-3">${msg.get("admin.rib.current")}</h2>
              @if(ribAvailable)
                <p class="mb-3">${msg.get("admin.rib.available")}@if(ribSize != null) (${ribSize})@endif</p>
                <div class="d-flex flex-wrap gap-2 mb-3">
                  <a class="btn btn-outline-primary" href="${contextPath}/rib" target="_blank">${msg.get("payment.downloadRIB")}</a>
                  <button class="btn btn-outline-danger" type="button" id="rib-delete-btn">${msg.get("action.remove")}</button>
                </div>
              @else
                <p class="text-body-secondary">${msg.get("admin.rib.missing")}</p>
              @endif
              <form id="rib-upload-form">
                <div class="mb-3">
                  <label class="form-label" for="rib-file">${msg.get("admin.rib.fileLabel")}</label>
                  <input class="form-control" type="file" id="rib-file" name="file" accept="application/pdf,.pdf" required>
                </div>
                <button class="btn btn-primary" type="submit" id="rib-upload-btn">${msg.get("admin.rib.upload")}</button>
              </form>
            </div>
          </div>
        </div>

        <div class="tab-pane fade ${("stripe".equals(tab) ? "show active" : "")}" id="stripe" role="tabpanel">
          <form method="post" action="${contextPath}/admin/org/config">
            <input type="hidden" name="tab" value="stripe">
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.stripePublic")}</label>
              <input class="form-control" name="stripe.public" value="${cfg.get("stripe.public") != null ? cfg.get("stripe.public") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.stripeSecret")}</label>
              @if((secretsConfigured.get("stripe.secret") != null ? secretsConfigured.get("stripe.secret") : false))<span class="badge text-bg-secondary mb-1">${msg.get("admin.secret.configured")}</span>@else<span class="badge text-bg-warning mb-1">${msg.get("admin.secret.missing")}</span>@endif
              <input class="form-control" type="password" name="stripe.secret" autocomplete="new-password" placeholder="${msg.get("admin.secret.placeholder")}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.stripePrefix")}</label>
              <input class="form-control" name="stripe.keyprefix" value="${cfg.get("stripe.keyprefix") != null ? cfg.get("stripe.keyprefix") : ""}"></div>
            <div class="form-check mb-3">
              @if(cfg.get("stripe.simuled") != null && "true".equals(String.valueOf(cfg.get("stripe.simuled"))))
              <input class="form-check-input" type="checkbox" name="stripe.simuled" value="true" id="stripe.simuled" checked>
              @else
              <input class="form-check-input" type="checkbox" name="stripe.simuled" value="true" id="stripe.simuled">
              @endif
              <label class="form-check-label" for="stripe.simuled">${msg.get("admin.field.stripeSimuled")}</label>
            </div>
            <button class="btn btn-primary" type="submit">${msg.get("action.save")}</button>
          </form>
        </div>

        <div class="tab-pane fade ${("oauth".equals(tab) ? "show active" : "")}" id="oauth" role="tabpanel">
          <form method="post" action="${contextPath}/admin/org/config">
            <input type="hidden" name="tab" value="oauth">
            <p class="text-body-secondary">${msg.get("help.adminOauth")}</p>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthClientId")}</label>
              <input class="form-control" name="oauth.clientId" value="${cfg.get("oauth.clientId") != null ? cfg.get("oauth.clientId") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthClientSecret")}</label>
              @if((secretsConfigured.get("oauth.clientSecret") != null ? secretsConfigured.get("oauth.clientSecret") : false))<span class="badge text-bg-secondary mb-1">${msg.get("admin.secret.configured")}</span>@else<span class="badge text-bg-warning mb-1">${msg.get("admin.secret.missing")}</span>@endif
              <input class="form-control" type="password" name="oauth.clientSecret" autocomplete="new-password" placeholder="${msg.get("admin.secret.placeholder")}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthAuthorizationUrl")}</label>
              <input class="form-control" name="oauth.authorizationUrl" value="${cfg.get("oauth.authorizationUrl") != null ? cfg.get("oauth.authorizationUrl") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthTokenUrl")}</label>
              <input class="form-control" name="oauth.tokenUrl" value="${cfg.get("oauth.tokenUrl") != null ? cfg.get("oauth.tokenUrl") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthScope")}</label>
              <input class="form-control" name="oauth.scope" value="${cfg.get("oauth.scope") != null ? cfg.get("oauth.scope") : ""}" placeholder="openid email profile"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthLogoutUrl")}</label>
              <input class="form-control" name="oauth.logoutUrl" value="${cfg.get("oauth.logoutUrl") != null ? cfg.get("oauth.logoutUrl") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthAccountUrl")}</label>
              <input class="form-control" name="oauth.accountUrl" value="${cfg.get("oauth.accountUrl") != null ? cfg.get("oauth.accountUrl") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.oauthUserinfoUrl")}</label>
              <input class="form-control" name="oauth.userinfoUrl" value="${cfg.get("oauth.userinfoUrl") != null ? cfg.get("oauth.userinfoUrl") : ""}"></div>
            <button class="btn btn-primary" type="submit">${msg.get("action.save")}</button>
          </form>
        </div>

        <div class="tab-pane fade ${("system".equals(tab) ? "show active" : "")}" id="system" role="tabpanel">
          <form method="post" action="${contextPath}/admin/org/config">
            <input type="hidden" name="tab" value="system">
            <div class="alert alert-warning">${msg.get("admin.dbRestartWarning")}</div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.baseurl")}</label>
              <input class="form-control" name="baseurl" value="${cfg.get("baseurl") != null ? cfg.get("baseurl") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.jwtKey")}</label>
              @if((secretsConfigured.get("jwt.key") != null ? secretsConfigured.get("jwt.key") : false))<span class="badge text-bg-secondary mb-1">${msg.get("admin.secret.configured")}</span>@else<span class="badge text-bg-warning mb-1">${msg.get("admin.secret.missing")}</span>@endif
              <input class="form-control" type="password" name="jwt.key" autocomplete="new-password" placeholder="${msg.get("admin.secret.placeholder")}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.mailUser")}</label>
              <input class="form-control" name="mail.USER_NAME" value="${cfg.get("mail.USER_NAME") != null ? cfg.get("mail.USER_NAME") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.mailPassword")}</label>
              @if((secretsConfigured.get("mail.PASSWORD") != null ? secretsConfigured.get("mail.PASSWORD") : false))<span class="badge text-bg-secondary mb-1">${msg.get("admin.secret.configured")}</span>@else<span class="badge text-bg-warning mb-1">${msg.get("admin.secret.missing")}</span>@endif
              <input class="form-control" type="password" name="mail.PASSWORD" autocomplete="new-password" placeholder="${msg.get("admin.secret.placeholder")}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.dbHost")}</label>
              <input class="form-control" name="db.host" value="${cfg.get("db.host") != null ? cfg.get("db.host") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.dbName")}</label>
              <input class="form-control" name="db.name" value="${cfg.get("db.name") != null ? cfg.get("db.name") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.dbUser")}</label>
              <input class="form-control" name="db.user" value="${cfg.get("db.user") != null ? cfg.get("db.user") : ""}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.dbPass")}</label>
              @if((secretsConfigured.get("db.pass") != null ? secretsConfigured.get("db.pass") : false))<span class="badge text-bg-secondary mb-1">${msg.get("admin.secret.configured")}</span>@else<span class="badge text-bg-warning mb-1">${msg.get("admin.secret.missing")}</span>@endif
              <input class="form-control" type="password" name="db.pass" autocomplete="new-password" placeholder="${msg.get("admin.secret.placeholder")}"></div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.dbType")}</label>
              <select class="form-select" name="db.type">
                @if(cfg.get("db.type") == null || !"postgres".equals(String.valueOf(cfg.get("db.type"))))
                <option value="h2" selected>h2</option>
                @else
                <option value="h2">h2</option>
                @endif
                @if(cfg.get("db.type") != null && "postgres".equals(String.valueOf(cfg.get("db.type"))))
                <option value="postgres" selected>postgres</option>
                @else
                <option value="postgres">postgres</option>
                @endif
              </select>
            </div>
            <div class="form-check mb-3">
              @if(cfg.get("db.h2.loadPostgresDump") != null && "true".equals(String.valueOf(cfg.get("db.h2.loadPostgresDump"))))
              <input class="form-check-input" type="checkbox" name="db.h2.loadPostgresDump" value="true" id="db.h2.loadPostgresDump" checked>
              @else
              <input class="form-check-input" type="checkbox" name="db.h2.loadPostgresDump" value="true" id="db.h2.loadPostgresDump">
              @endif
              <label class="form-check-label" for="db.h2.loadPostgresDump">${msg.get("admin.field.dbH2LoadDump")}</label>
            </div>
            <div class="mb-3"><label class="form-label">${msg.get("admin.field.dbH2DumpFile")}</label>
              <input class="form-control" name="db.h2.postgresDumpFile" value="${cfg.get("db.h2.postgresDumpFile") != null ? cfg.get("db.h2.postgresDumpFile") : ""}"></div>
            <button class="btn btn-primary" type="submit">${msg.get("action.save")}</button>
          </form>
        </div>
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    <script type="text/javascript">
      function bindUpload(formId, inputId, buttonId, url, contentType) {
        const form = document.getElementById(formId);
        if (!form) return;
        const fileInput = document.getElementById(inputId);
        const button = document.getElementById(buttonId);
        const statusEl = document.getElementById('rib-status');
        form.addEventListener('submit', async (event) => {
          event.preventDefault();
          const file = fileInput.files[0];
          if (!file) {
            return;
          }
          button.disabled = true;
          try {
            const res = await fetch(url, {
              method: 'POST',
              headers: { 'Content-Type': contentType },
              body: file
            });
            if (!res.ok) {
              const text = await res.text();
              statusEl.className = 'alert mb-3 alert-danger';
              statusEl.textContent = text || '${msg.get("admin.uploadFailed")}';
              statusEl.classList.remove('d-none');
              button.disabled = false;
              return;
            }
            window.location = res.url;
          } catch (e) {
            statusEl.className = 'alert mb-3 alert-danger';
            statusEl.textContent = '${msg.get("admin.uploadFailed")}';
            statusEl.classList.remove('d-none');
            button.disabled = false;
          }
        });
      }
      function bindDelete(buttonId, url, confirmMessage) {
        const button = document.getElementById(buttonId);
        if (!button) return;
        const statusEl = document.getElementById('rib-status');
        button.addEventListener('click', async () => {
          if (!window.confirm(confirmMessage)) {
            return;
          }
          button.disabled = true;
          try {
            const res = await fetch(url, { method: 'DELETE' });
            if (!res.ok) {
              const text = await res.text();
              statusEl.className = 'alert mb-3 alert-danger';
              statusEl.textContent = text || '${msg.get("admin.deleteFailed")}';
              statusEl.classList.remove('d-none');
              button.disabled = false;
              return;
            }
            window.location = res.url;
          } catch (e) {
            statusEl.className = 'alert mb-3 alert-danger';
            statusEl.textContent = '${msg.get("admin.deleteFailed")}';
            statusEl.classList.remove('d-none');
            button.disabled = false;
          }
        });
      }
      bindUpload('rib-upload-form', 'rib-file', 'rib-upload-btn', '${contextPath}/admin/org/rib', 'application/octet-stream');
      bindUpload('logo-upload-form', 'logo-file', 'logo-upload-btn', '${contextPath}/admin/org/logo', 'application/octet-stream');
      bindUpload('background-upload-form', 'background-file', 'background-upload-btn', '${contextPath}/admin/org/background', 'application/octet-stream');
      bindDelete('logo-delete-btn', '${contextPath}/admin/org/logo', '${msg.get("action.confirm")}');
      bindDelete('background-delete-btn', '${contextPath}/admin/org/background', '${msg.get("action.confirm")}');
      bindDelete('rib-delete-btn', '${contextPath}/admin/org/rib', '${msg.get("action.confirm")}');

      const hash = (window.location.hash || '').replace('#', '');
      if (hash) {
        const trigger = document.querySelector('[data-bs-target="#' + hash + '"]');
        if (trigger && window.bootstrap) {
          window.bootstrap.Tab.getOrCreateInstance(trigger).show();
        }
      }
      document.querySelectorAll('#org-tabs button[data-bs-toggle="tab"]').forEach((btn) => {
        btn.addEventListener('shown.bs.tab', (event) => {
          const target = event.target.getAttribute('data-bs-target');
          if (target) {
            history.replaceState(null, '', target);
          }
        });
      });
    </script>
  </body>
</html>