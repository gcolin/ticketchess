@param String title
@param String helpText = null
@param String icon = null
<div class="admin-table-panel mb-4">
  @if(icon != null && !icon.isBlank())
    <div class="d-flex align-items-start gap-3">
      <i class="bi ${icon} fs-2 text-primary"></i>
      <div>
        <h1 class="h3 mb-1">${title}</h1>
        @if(helpText != null && !helpText.isBlank())
          <p class="text-body-secondary mb-0">${helpText}</p>
        @endif
      </div>
    </div>
  @else
    <h1 class="mb-2">${title}</h1>
    @if(helpText != null && !helpText.isBlank())
      <p class="text-body-secondary mb-0">${helpText}</p>
    @endif
  @endif
</div>
