@import gg.jte.Content
@param Content content
@param String heading = null
@param String subheading = null
<div class="admin-table-panel mb-4">
  @if(heading != null && !heading.isBlank())
    <h2 class="h5 mb-2">${heading}</h2>
  @endif
  @if(subheading != null && !subheading.isBlank())
    <p class="text-body-secondary mb-3">${subheading}</p>
  @endif
  ${content}
</div>
