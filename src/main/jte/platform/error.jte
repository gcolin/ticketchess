@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<String> errors
@param Integer statusCode
@param String statusMessage
<!DOCTYPE html>
!{if (statusCode == null) statusCode = 404;}
!{if (statusMessage == null) statusMessage = "Not Found";}
!{title = String.valueOf(statusCode) + " – " + statusMessage;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
  <main class="container flex-fill py-5">
    <div class="bg-body p-5 rounded text-center error-page">
      <div class="mb-4">
        <span class="display-1 fw-bold text-danger">${statusCode}</span>
      </div>
      <h1 class="h3 mb-3">${statusMessage}</h1>
      @if((errors != null && !errors.isEmpty()))
        <ul class="list-unstyled text-start d-inline-block mb-4">
          @for(var errMsg : errors)
            <li class="text-danger"><i class="bi bi-exclamation-circle me-1"></i>${errMsg}</li>
          @endfor
        </ul>
      @endif
      <div>
        <a href="javascript:history.back()" class="btn btn-secondary me-2">
          <i class="bi bi-arrow-left me-1"></i>Retour
        </a>
        <a href="${contextPath}/" class="btn btn-primary">
          <i class="bi bi-house me-1"></i>Accueil
        </a>
      </div>
    </div>
  </main>
  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>