@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param java.util.List<com.github.gcolin.event.EventAdminGroupNode> groups
@param java.util.List<com.github.gcolin.platform.SelectItem> seasons
@param Integer seasonId
!{i18ntitle = msg.get("page.adminEvents");}
!{title = i18ntitle;}
!{page = "admin";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3 mt-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.adminEvents")}</li>
        </ol>
      </nav>

      <div class="card mb-3">
        <div class="card-body">
          <h1 class="h4 mb-2">${msg.get("heading.adminEvents")}</h1>
          <p class="text-body-secondary mb-0">${msg.get("help.adminEvents")}</p>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-body">
          <div class="row g-3">
            <div class="col-12 col-md-4">
              <div class="d-flex align-items-start gap-3">
                <div>
                  <a href="${contextPath}/eventgroup/new" class="btn btn-primary mb-2">
                    <i class="bi bi-diagram-3 me-1"></i>${msg.get("adminEvents.createEventGroup")}
                  </a>
                  <p class="text-body-secondary small mb-0">${msg.get("adminEvents.createEventGroupHelp")}</p>
                </div>
              </div>
            </div>
            <div class="col-12 col-md-4">
              <div class="d-flex align-items-start gap-3">
                <div>
                  <a href="${contextPath}/eventcollection/new" class="btn btn-primary mb-2">
                    <i class="bi bi-collection me-1"></i>${msg.get("adminEvents.createEventCollection")}
                  </a>
                  <p class="text-body-secondary small mb-0">${msg.get("adminEvents.createEventCollectionHelp")}</p>
                </div>
              </div>
            </div>
            @if(user.hasPermission("EVENT_CREATE"))
            <div class="col-12 col-md-4">
              <div class="d-flex align-items-start gap-3">
                <div>
                  <a href="${contextPath}/event/new" class="btn btn-primary mb-2">
                    <i class="bi bi-calendar-plus me-1"></i>${msg.get("adminEvents.createEvent")}
                  </a>
                  <p class="text-body-secondary small mb-0">${msg.get("admin.tile.eventNewDesc")}</p>
                </div>
              </div>
            </div>
            @endif
          </div>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-body">
          <form method="get" action="${contextPath}/admin/events" class="row g-2 align-items-end mb-3">
            @template.platform.seasonFilter(msg=msg, seasons=seasons, seasonId=seasonId, selectId="admin_events_season", autoSubmit=false)
            <div class="col-12 col-md-auto">
              <button type="submit" class="btn btn-outline-primary">${msg.get("action.applyFilters")}</button>
            </div>
          </form>
          @if(groups.isEmpty())
            <p class="text-body-secondary mb-0">${msg.get("adminEvents.empty")}</p>
          @else
            <div class="d-flex justify-content-end mb-3">
              <div class="input-group input-group-sm" style="max-width: 420px;">
                <span class="input-group-text"><i class="bi bi-search"></i></span>
                <input id="admin-events-filter" class="form-control" type="search" placeholder="${msg.get("adminEvents.filterPlaceholder")}" aria-label="${msg.get("adminEvents.filterPlaceholder")}">
              </div>
            </div>
            <p id="admin-events-filter-no-results" class="text-body-secondary mb-0 d-none">${msg.get("adminEvents.filterNoResults")}</p>
            <ul class="list-unstyled mb-0 event-admin-tree" id="admin-events-tree">
              @for(var groupNode : groups)
                <li class="mb-3 event-admin-tree-group"
                    data-search="${!groupNode.isUngrouped() ? groupNode.getGroup().getName() + " " + groupNode.getGroup().getShortname() : msg.get("adminEvents.ungrouped")}">
                  <div class="d-flex align-items-center gap-2 fw-semibold">
                    <i class="bi bi-diagram-3 text-primary"></i>
                    @if(groupNode.isUngrouped())
                      <span>${msg.get("adminEvents.ungrouped")}</span>
                    @else
                      @template.event.eventAdminTreeGroupRow(contextPath=contextPath, msg=msg, user=user, group=groupNode.getGroup())
                    @endif
                  </div>

                  @if(!groupNode.getCollections().isEmpty() || !groupNode.getEvents().isEmpty())
                    <ul class="list-unstyled ms-4 mt-2 mb-0">
                      @for(var collectionNode : groupNode.getCollections())
                        <li class="mb-2 event-admin-tree-collection">
                          <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-collection text-secondary"></i>
                            @template.event.eventAdminTreeCollectionRow(contextPath=contextPath, msg=msg, user=user, collection=collectionNode.getCollection())
                          </div>
                          <ul class="list-unstyled ms-4 mt-1 mb-0">
                            @for(var event : collectionNode.getEvents())
                              <li class="py-1 border-bottom border-light-subtle event-admin-tree-event"
                                  data-search="${event.getName()} ${event.getStartDate() != null ? msg.formatDate(event.getStartDate()) : ""} ${!groupNode.isUngrouped() ? groupNode.getGroup().getName() + " " + groupNode.getGroup().getShortname() : msg.get("adminEvents.ungrouped")} ${collectionNode.getCollection().getName()} ${event.getId()}">
                                @template.event.eventAdminTreeEventRow(contextPath=contextPath, msg=msg, user=user, event=event)
                              </li>
                            @endfor
                          </ul>
                        </li>
                      @endfor

                      @for(var event : groupNode.getEvents())
                        <li class="py-1 border-bottom border-light-subtle event-admin-tree-event"
                            data-search="${event.getName()} ${event.getStartDate() != null ? msg.formatDate(event.getStartDate()) : ""} ${!groupNode.isUngrouped() ? groupNode.getGroup().getName() + " " + groupNode.getGroup().getShortname() : msg.get("adminEvents.ungrouped")} ${event.getId()}">
                          @template.event.eventAdminTreeEventRow(contextPath=contextPath, msg=msg, user=user, event=event)
                        </li>
                      @endfor
                    </ul>
                  @else
                    <p class="text-body-secondary small ms-4 mb-0 mt-1 event-admin-tree-empty">${msg.get("adminEvents.noEvents")}</p>
                  @endif
                </li>
              @endfor
            </ul>
          @endif
        </div>
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
    @if(!groups.isEmpty())
    <script type="text/javascript">
      (function () {
        const input = document.getElementById('admin-events-filter');
        const tree = document.getElementById('admin-events-tree');
        const noResults = document.getElementById('admin-events-filter-no-results');
        if (!input || !tree) {
          return;
        }

        const eventRows = Array.from(tree.querySelectorAll('.event-admin-tree-event'));
        const collectionRows = Array.from(tree.querySelectorAll('.event-admin-tree-collection'));
        const groupRows = Array.from(tree.querySelectorAll('.event-admin-tree-group'));

        function normalize(value) {
          return (value || '')
              .toLowerCase()
              .normalize('NFD')
              .replace(/[\u0300-\u036f]/g, '');
        }

        function matchesTerms(text, terms) {
          return terms.length === 0 || terms.every((term) => text.includes(term));
        }

        function applyFilter() {
          const terms = normalize(input.value).split(/\s+/).filter(Boolean);
          let visibleEvents = 0;

          eventRows.forEach((row) => {
            const text = normalize(row.getAttribute('data-search') || row.textContent);
            const match = matchesTerms(text, terms);
            row.classList.toggle('d-none', !match);
            if (match) {
              visibleEvents++;
            }
          });

          collectionRows.forEach((row) => {
            const events = row.querySelectorAll('.event-admin-tree-event');
            const hasVisible = Array.from(events).some((event) => !event.classList.contains('d-none'));
            row.classList.toggle('d-none', !hasVisible);
          });

          let visibleGroups = 0;
          groupRows.forEach((row) => {
            const events = row.querySelectorAll('.event-admin-tree-event');
            const hasVisibleEvents = Array.from(events).some((event) => !event.classList.contains('d-none'));
            const groupText = normalize(row.getAttribute('data-search') || row.textContent);
            const show = hasVisibleEvents || (events.length === 0 && matchesTerms(groupText, terms));
            row.classList.toggle('d-none', !show);
            if (show) {
              visibleGroups++;
            }
          });

          if (noResults) {
            noResults.classList.toggle('d-none', terms.length === 0 || visibleEvents > 0 || visibleGroups > 0);
            tree.classList.toggle('d-none', terms.length > 0 && visibleEvents === 0 && visibleGroups === 0);
          }
        }

        input.addEventListener('input', applyFilter);
        applyFilter();
      })();
    </script>
    @endif
  </body>
</html>
