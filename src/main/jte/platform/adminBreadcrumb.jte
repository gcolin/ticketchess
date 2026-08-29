@import gg.jte.Content
@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param String activePageTitle
@param Boolean adminHub = false
@param Content trail = null
<nav aria-label="breadcrumb" class="admin-breadcrumb mb-3">
  <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
    <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
    @if(adminHub)
      <li class="breadcrumb-item active" aria-current="page">${msg.get("menu.admin")}</li>
    @else
      <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
      @if(trail != null)
        ${trail}
      @endif
      <li class="breadcrumb-item active" aria-current="page">${activePageTitle}</li>
    @endif
  </ol>
</nav>
