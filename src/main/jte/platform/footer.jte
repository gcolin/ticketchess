@param String contextPath
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.Messages msg
<footer class="bg-light border-top mt-5">
  <div class="container py-4">
    <div class="row align-items-center">

      <div class="col-md-6 text-center text-md-start mb-2 mb-md-0">
        <span class="text-muted">
          © 2026 <strong>${pageConfig.getOrgName() != null ? pageConfig.getOrgName() : ""}</strong>
        </span>
      </div>

      <div class="col-md-6 text-center text-md-end">
        <a href="${contextPath}/mentions" class="text-muted text-decoration-none me-3">${msg.get("footer.legal")}</a>
        <a href="${pageConfig.getContactUrl()}" class="text-muted text-decoration-none me-3">${msg.get("footer.contact")}</a>
        @if(pageConfig.getSourceUrl() != null && !pageConfig.getSourceUrl().isBlank())
          <a href="${pageConfig.getSourceUrl()}" class="text-muted text-decoration-none me-3" target="_blank" rel="noopener noreferrer">${msg.get("footer.source")}</a>
          <a href="https://www.gnu.org/licenses/agpl-3.0.html" class="text-muted text-decoration-none" target="_blank" rel="noopener noreferrer">${msg.get("footer.license")}</a>
        @endif
      </div>

    </div>
  </div>
</footer>
