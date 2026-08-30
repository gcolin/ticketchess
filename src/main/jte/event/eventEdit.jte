@param String contextPath
@param String error
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param Object collection
@param com.github.gcolin.event.Event event
@param com.github.gcolin.event.EventInfo eventInfo
@param java.util.Map<String, String> eventOptions
@param com.github.gcolin.event.EventCollection eventCollection
@param java.util.List<com.github.gcolin.event.EventCollection> eventCollections
@param com.github.gcolin.event.EventGroup eventGroup
@param java.util.List<com.github.gcolin.event.EventGroup> eventGroups
@param java.util.List<com.github.gcolin.event.Event> events
@param Object item
@param Object message
@param com.github.gcolin.membership.MembershipOption option
@param java.util.List<com.github.gcolin.membership.MembershipOption> options
@param Object p
@param String placeholder
@param String pointageValue
@param com.github.gcolin.membership.LicensePrice price
@param String selectedPairing
@param String status
@param Object type
!{i18ntitle = msg.get("action.edit");}
!{title = i18ntitle;}
!{if (eventOptions == null) eventOptions = new java.util.HashMap<String, String>();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "event";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <div class="container flex-fill">
        <nav aria-label="breadcrumb" class="mb-3">
            <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
                <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
                <li class="breadcrumb-item"><a href="${contextPath}/event">${msg.get("breadcrumb.events")}</a></li>
                @if(event.getId() != null)
                <li class="breadcrumb-item"><a href="${contextPath}/event/${event.getId()}">${event.getName() != null ? event.getName() : String.valueOf(event.getId())}</a></li>
                @endif
                <li class="breadcrumb-item active" aria-current="page">@if(event.getId() != null)${msg.get("action.editEvent")}@else${msg.get("action.newEvent")}@endif</li>
            </ol>
        </nav>
        <div class="bg-body p-3 rounded mb-3">
        <ul class="nav nav-tabs" id="myTab" role="tablist">
            <li class="nav-item" role="presentation">
                <button class="nav-link active" id="home-tab" data-bs-toggle="tab"
                    data-bs-target="#general" type="button" role="tab" aria-controls="general"
                    aria-selected="true">
                    ${msg.get("event.generalinfo")}
                </button>
            </li>
            @if(event.getId() != null)
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="profile-tab" data-bs-toggle="tab"
                    data-bs-target="#description" type="button" role="tab" aria-controls="description"
                    aria-selected="false">
                    ${msg.get("event.descriptionlabel")}
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="options-tab" data-bs-toggle="tab"
                    data-bs-target="#options" type="button" role="tab" aria-controls="options"
                    aria-selected="false">
                    ${msg.get("event.optionslabel")}
                </button>
            </li>
            @endif
        </ul>
        <div class="tab-content" id="myTabContent">
            <div class="tab-pane fade show active" id="general" role="tabpanel"
                aria-labelledby="general-tab">
                <div class="row justify-content-center mt-3">
                    <form method="post" action="${contextPath}/event" class="js-event-edit-form" enctype="application/x-www-form-urlencoded">
                        @if(success != null && "save".equals(success))
                        <div class="alert alert-success" role="alert" data-submit-success>
                            ${msg.get("message.saveSuccess")}
                        </div>
                        @endif

                        <!-- Event ID (hidden for edit) -->
                        <input type="hidden" name="eventId" value="@if(event.getId() != null)${String.valueOf(event.getId())}@endif">

                        <!-- Event Name -->
                        <div class="mb-3">
                            <label class="form-label">
                                ${msg.get("event.name")}
                            </label>
                            <input id="event_name" type="text" name="name" value="${event.getName() != null ? event.getName() : ""}" class="form-control" maxlength="50" required>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">
                                ${msg.get("label.eventGroup")}
                            </label>
                            <select id="event_group" name="eventgroup" class="form-select">
                                <option value="">-</option>
                                @if(eventGroups != null)
                                @for(var eg : eventGroups)
                                @if(java.util.Objects.nonNull(event.getEventGroup()) && event.getEventGroup().getId().equals(eg.getId()))
                                <option value="${eg.getId()}" selected>${eg.getName()}</option>
                                @else
                                <option value="${eg.getId()}">${eg.getName()}</option>
                                @endif
                                @endfor
                                @endif
                            </select>
                            <small class="form-text text-body-secondary">${msg.get("help.eventGroupSelection")}</small>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">
                                Event collection
                            </label>
                            <select id="event_eventcollection" name="eventcollection" class="form-select">
                                <option value="">-</option>
                                @if(eventCollections != null)
                                @for(var ec : eventCollections)
                                @if(java.util.Objects.nonNull(event.getEventCollection()) && event.getEventCollection().getId().equals(ec.getId()))
                                <option value="${ec.getId()}" selected>${ec.getName()}</option>
                                @else
                                <option value="${ec.getId()}">${ec.getName()}</option>
                                @endif
                                @endfor
                                @endif
                            </select>
                            <small class="form-text text-body-secondary">${msg.get("help.eventCollectionSelection")}</small>
                        </div>

                        <!-- Dates -->
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label">
                                    ${msg.get("event.start")}
                                </label>
                                <input id="event_startdate" type="date" name="startDate" value="@if(event.getStartDateAsDate() != null)${event.getStartDateAsDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}@endif" class="form-control" required>
                            </div>

                            <div class="col-md-6 mb-3">
                                <label class="form-label">
                                    ${msg.get("event.end")}
                                </label>
                                <input id="event_enddate" type="date" name="endDate" value="@if(event.getEndDateAsDate() != null)${event.getEndDateAsDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}@endif" class="form-control" required>
                            </div>
                        </div>

                        <!-- Event Type -->
                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.type")}</label>
                            <select id="event_type" name="eventType" class="form-select" required>
                                @if(event.getEventType() != null && "STANDARD".equals(event.getEventType().name()))
                                <option value="STANDARD" selected>${msg.get("type.STANDARD")}</option>
                                @else
                                <option value="STANDARD">${msg.get("type.STANDARD")}</option>
                                @endif
                                @if(event.getEventType() != null && "RAPID".equals(event.getEventType().name()))
                                <option value="RAPID" selected>${msg.get("type.RAPID")}</option>
                                @else
                                <option value="RAPID">${msg.get("type.RAPID")}</option>
                                @endif
                                @if(event.getEventType() != null && "BLITZ".equals(event.getEventType().name()))
                                <option value="BLITZ" selected>${msg.get("type.BLITZ")}</option>
                                @else
                                <option value="BLITZ">${msg.get("type.BLITZ")}</option>
                                @endif
                            </select>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.price")}</label>
                            <input id="event_price" type="number" step="0.01" name="price"
                                   class="form-control" value="${event.getPrice() != null ? event.getPrice() : 0}" required>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.youngPrice")}</label>
                            <input id="event_price_young" type="number" step="0.01" name="youngprice"
                                   class="form-control" value="${event.getYoungPrice() != null ? event.getYoungPrice() : 0}" required>
                        </div>

                        <!-- Event Status -->
                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.status")}</label>
                            <select id="event_status" name="status" class="form-select" required>
                                @if(event.getStatus() != null && "DRAFT".equals(event.getStatus().name()))
                                <option value="DRAFT" selected>${msg.get("status.DRAFT")}</option>
                                @else
                                <option value="DRAFT">${msg.get("status.DRAFT")}</option>
                                @endif
                                @if(event.getStatus() != null && "ACTIVE".equals(event.getStatus().name()))
                                <option value="ACTIVE" selected>${msg.get("status.ACTIVE")}</option>
                                @else
                                <option value="ACTIVE">${msg.get("status.ACTIVE")}</option>
                                @endif
                                @if(event.getStatus() != null && "ONGOING".equals(event.getStatus().name()))
                                <option value="ONGOING" selected>${msg.get("status.ONGOING")}</option>
                                @else
                                <option value="ONGOING">${msg.get("status.ONGOING")}</option>
                                @endif
                                @if(event.getStatus() != null && "COMPLETED".equals(event.getStatus().name()))
                                <option value="COMPLETED" selected>${msg.get("status.COMPLETED")}</option>
                                @else
                                <option value="COMPLETED">${msg.get("status.COMPLETED")}</option>
                                @endif
                            </select>
                        </div>

                        <!-- PAPI Fields -->
                        <div class="row">
                            <div class="col-md-3 mb-3">
                                <label class="form-label">${msg.get("event.rondes")}</label>
                                <input id="event_rondes" type="number" name="rondes"
                                       value="${eventOptions.getOrDefault("ROUNDS", "7")}"
                                       class="form-control">
                            </div>
                            <div class="col-md-3 mb-3">
                                <label class="form-label">${msg.get("event.cadence")}</label>
                                <input id="event_cadence" type="text" name="cadence"
                                       value="${eventOptions.get("CADENCE") != null ? eventOptions.get("CADENCE") : ""}" class="form-control"
                                       placeholder="ex: 1h30+30s">
                            </div>
                            <div class="col-md-3 mb-3">
!{selectedPairing = eventOptions.getOrDefault("PAIRING", "Standard");}
                                <label class="form-label" for="event_pairing">${msg.get("event.pairing")}</label>
                                <select id="event_pairing" name="pairing" class="form-select">
                                    @if("Standard".equals(selectedPairing))
                                    <option value="Standard" selected>${msg.get("event.pairing.Standard")}</option>
                                    @else
                                    <option value="Standard">${msg.get("event.pairing.Standard")}</option>
                                    @endif
                                    @if("Haley".equals(selectedPairing))
                                    <option value="Haley" selected>${msg.get("event.pairing.Haley")}</option>
                                    @else
                                    <option value="Haley">${msg.get("event.pairing.Haley")}</option>
                                    @endif
                                    @if("HaleySoft".equals(selectedPairing))
                                    <option value="HaleySoft" selected>${msg.get("event.pairing.HaleySoft")}</option>
                                    @else
                                    <option value="HaleySoft">${msg.get("event.pairing.HaleySoft")}</option>
                                    @endif
                                    @if("SAD".equals(selectedPairing))
                                    <option value="SAD" selected>${msg.get("event.pairing.SAD")}</option>
                                    @else
                                    <option value="SAD">${msg.get("event.pairing.SAD")}</option>
                                    @endif
                                    @if("Nicois".equals(selectedPairing))
                                    <option value="Nicois" selected>${msg.get("event.pairing.Nicois")}</option>
                                    @else
                                    <option value="Nicois">${msg.get("event.pairing.Nicois")}</option>
                                    @endif
                                    @if("Berger".equals(selectedPairing))
                                    <option value="Berger" selected>${msg.get("event.pairing.Berger")}</option>
                                    @else
                                    <option value="Berger">${msg.get("event.pairing.Berger")}</option>
                                    @endif
                                </select>
                            </div>
                            <div class="col-md-3 mb-3">
                                <label class="form-label">${msg.get("event.maxSubscriptions")}</label>
                                <input id="event_maxsubscriptions" type="number" name="maxSubscriptions"
                                       value="${eventOptions.get("MAX_SUBSCRIPTIONS") != null ? eventOptions.get("MAX_SUBSCRIPTIONS") : ""}" class="form-control" min="1">
                            </div>
                        </div>

                        <!-- Submit -->
                        <div class="d-flex justify-content-end">
                            <button id="event_submit" type="submit" class="btn btn-primary">
                                &#128190; ${msg.get("action.save")}
                            </button>
                        </div>

                    </form>
                </div>
            </div>
            @if(event.getId() != null)
            <div class="tab-pane fade" id="description" role="tabpanel"
                aria-labelledby="description-tab">
                <div class="row justify-content-center mt-3">
                    <form method="post" action="${contextPath}/event" class="js-event-edit-form" enctype="application/x-www-form-urlencoded">

                        <div class="alert alert-danger d-none" role="alert" data-submit-error>
                            ${msg.get("error.serverError")}
                        </div>
                        <div class="alert alert-success d-none" role="alert" data-submit-success>
                            ${msg.get("message.saveSuccess")}
                        </div>

                        <!-- Event ID (hidden for edit) -->
                        <input type="hidden" name="eventId" value="${event.getId()}">
                        <input type="hidden" name="descriptionOnly" value="true">
                        <!-- Event Description -->
                        <div class="row mb-3">
                            <div class="col-lg-6 mb-3 mb-lg-0">
                                <label class="form-label" for="event_description">
                                    ${msg.get("event.description")}
                                </label>
!{placeholder = msg.get("event.descriptionplaceholder");}
                                <textarea id="event_description" name="description" class="form-control" rows="20"
                                    placeholder="${placeholder}">@if(eventInfo != null && eventInfo.getDescription() != null)${eventInfo.getDescription()}@endif</textarea>
                            </div>
                            <div class="col-lg-6">
                                <label class="form-label" for="event_description_preview">
                                    ${msg.get("mail.preview")}
                                </label>
                                <div id="event_description_preview" class="border rounded p-3 bg-body-secondary" style="min-height: 420px; overflow: auto;"></div>
                            </div>
                        </div>

                        <!-- Submit -->
                        <div class="d-flex justify-content-end">
                            <button id="event_description_submit" type="submit" class="btn btn-primary">
                                &#128190; ${msg.get("action.save")}
                            </button>
                        </div>

                    </form>
                </div>
            </div>
            <div class="tab-pane fade" id="options" role="tabpanel"
                aria-labelledby="options-tab">
                <div class="row justify-content-center mt-3">
                    <form method="post" action="${contextPath}/event" class="js-event-edit-form" enctype="application/x-www-form-urlencoded">

                        <div class="alert alert-danger d-none" role="alert" data-submit-error>
                            ${msg.get("error.serverError")}
                        </div>
                        <div class="alert alert-success d-none" role="alert" data-submit-success>
                            ${msg.get("message.saveSuccess")}
                        </div>

                        <input type="hidden" name="eventId" value="${event.getId()}">
                        <input type="hidden" name="optionsOnly" value="true">
                       
                        <div class="mb-3">
                            <label class="form-label">MIN_ELO</label>
                            <input id="event_min_elo" type="number" name="minElo"
                                   value="${eventOptions.get("MIN_ELO") != null ? eventOptions.get("MIN_ELO") : ""}" class="form-control">
                        </div>

                        <div class="mb-3">
                            <label class="form-label">MAX_ELO</label>
                            <input id="event_max_elo" type="number" name="maxElo"
                                   value="${eventOptions.get("MAX_ELO") != null ? eventOptions.get("MAX_ELO") : ""}" class="form-control">
                        </div>

                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.ffeId")}</label>
                            <div class="input-group">
                              <input id="event_ffe_id" type="text" name="ffeId"
                                     value="${eventOptions.get("FFE_ID") != null ? eventOptions.get("FFE_ID") : ""}" class="form-control">
                              @if(eventOptions.get("FFE_ID") != null && !eventOptions.get("FFE_ID").toString().isBlank())
                                <a id="ffe-link" class="btn  btn-secondary"
                                   href="https://www.echecs.asso.fr/FicheTournoi.aspx?Ref=${java.net.URLEncoder.encode(String.valueOf(eventOptions.get("FFE_ID")), java.nio.charset.StandardCharsets.UTF_8)}"
                                   target="_blank" rel="noopener noreferrer"
                                   title="${msg.get("event.ffeLink")}">
                                  <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                              @endif
                            </div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.ffePassword")}</label>
                            <input id="event_ffe_password" type="text" name="ffePassword"
                                   value="${eventOptions.get("FFE_PASSWORD") != null ? eventOptions.get("FFE_PASSWORD") : ""}" class="form-control" autocomplete="new-password">
                        </div>

                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.chessEventUser")}</label>
                            <input id="event_chess_event_user" type="text" name="chessEventUser"
                                   value="${eventOptions.get("CHESS_EVENT_USER") != null ? eventOptions.get("CHESS_EVENT_USER") : ""}" class="form-control" autocomplete="username">
                            <div class="form-text">${msg.get("event.chessEventUser.help")}</div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">${msg.get("event.chessEventPassword")}</label>
                            <input id="event_chess_event_password" type="text" name="chessEventPassword"
                                   value="${eventOptions.get("CHESS_EVENT_PASSWORD") != null ? eventOptions.get("CHESS_EVENT_PASSWORD") : ""}" class="form-control" autocomplete="new-password">
                            <div class="form-text">${msg.get("event.chessEventPassword.help")}</div>
                        </div>

                        <div class="mb-3 form-check">
!{pointageValue = eventOptions.getOrDefault("POINTAGE", "0");}
                            @if("1".equals(pointageValue))
                            <input type="checkbox" class="form-check-input" id="event_pointage"
                                   name="pointage" value="1" checked>
                            @else
                            <input type="checkbox" class="form-check-input" id="event_pointage"
                                   name="pointage" value="1">
                            @endif
                            <label class="form-check-label" for="event_pointage">${msg.get("event.pointage")}</label>
                        </div>

                        <div class="d-flex justify-content-end">
                            <button id="event_options_submit" type="submit" class="btn btn-primary">
                                &#128190; ${msg.get("action.save")}
                            </button>
                        </div>

                    </form>
                </div>
            </div>
            @endif
         </div>
    </div>
</div>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
@if(event.getId() != null)
<script>
(function () {
  var descriptionEl = document.getElementById('event_description');
  var previewEl = document.getElementById('event_description_preview');
  if (!descriptionEl || !previewEl) {
    return;
  }

  var timer = null;

  function updatePreview() {
    var formData = new URLSearchParams();
    formData.append('description', descriptionEl.value || '');

    fetch('${contextPath}/event/${event.getId()}/description/preview', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData.toString()
    })
    .then(function (response) { return response.text(); })
    .then(function (html) {
      previewEl.innerHTML = html;
    });
  }

  function scheduleUpdate() {
    clearTimeout(timer);
    timer = setTimeout(updatePreview, 400);
  }

  descriptionEl.addEventListener('input', scheduleUpdate);

  var descriptionTab = document.getElementById('profile-tab');
  if (descriptionTab) {
    descriptionTab.addEventListener('shown.bs.tab', updatePreview);
  }

  updatePreview();
})();
</script>
@endif
</body>
</html>