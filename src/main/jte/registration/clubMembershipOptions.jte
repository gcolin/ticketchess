@param String contextPath
@param String error
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.player.IPlayer player
@param boolean manualPlayer
@param String firstname
@param String lastname
@param String birthdate
@param String category
@param String nrffe
@param String selectedLicenseType
@param boolean playerYoung
@param java.util.List<java.util.Map<String, Object>> licenses
@param java.util.List<java.util.Map<String, Object>> options
!{i18ntitle = msg.get("clubRegister.membershipOptions.title");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "clubregister";}
  @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)

  <main class="container flex-fill">
    <nav aria-label="breadcrumb" class="mb-3 mt-3">
      <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
        <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
        <li class="breadcrumb-item"><a href="${contextPath}/club-register?query=">${msg.get("page.clubRegister")}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${msg.get("clubRegister.membershipOptions.title")}</li>
      </ol>
    </nav>

    <div class="bg-body p-3 rounded mb-3">
      <h1 class="h3 mb-2">${msg.get("clubRegister.membershipOptions.title")}</h1>
      <p class="mb-0 text-body-secondary">
        @if(manualPlayer)
          ${firstname != null ? firstname : ""} ${lastname != null ? lastname : ""} ${category != null ? category : ""} - ${birthdate != null ? birthdate.replaceAll("^(\\d{4})-(\\d{2})-(\\d{2})(?:[T ].*)?$", "$3/$2/$1") : ""}
        @else
          ${player.getFirstname() != null ? player.getFirstname() : ""} ${player.getName() != null ? player.getName() : ""} ${player.getCategory() != null ? player.getCategory() : ""} - ${player.getBirthDate() != null ? player.getBirthDate().replaceAll("^(\\d{4})-(\\d{2})-(\\d{2})(?:[T ].*)?$", "$3/$2/$1") : ""} - ${player.getLicence() != null ? player.getLicence() : nrffe}
        @endif
      </p>
    </div>

    @if(error != null && "licenseRequired".equals(error))
      <div class="alert alert-danger" role="alert">${msg.get("clubRegister.membershipOptions.licenseRequired")}</div>
    @endif

    <div class="bg-body p-3 rounded mb-3">
      <form method="POST" action="${contextPath}/club-register/membership-options">
        <input type="hidden" name="nrffe" value="${nrffe}">
        @if(manualPlayer)
          <input type="hidden" name="lastname" value="${lastname != null ? lastname : ""}">
          <input type="hidden" name="firstname" value="${firstname != null ? firstname : ""}">
          <input type="hidden" name="birthdate" value="${birthdate != null ? birthdate : ""}">
        @endif

        <div class="list-group mb-3">
          <div class="list-group-item active">${msg.get("clubRegister.membershipOptions.licenseTitle")}</div>
          @for(var lic : licenses)
          <label class="list-group-item d-flex justify-content-between align-items-center" data-license-id="${String.valueOf(lic.get("id"))}">
            <span>
              @if(selectedLicenseType != null && selectedLicenseType.equals(String.valueOf(lic.get("name"))))
              <input class="form-check-input me-2 js-license" type="radio" name="licenseType" value="${String.valueOf(lic.get("name"))}" data-price-cents="${String.valueOf(lic.get("priceCents"))}" data-license-id="${String.valueOf(lic.get("id"))}" checked required>
              @else
              <input class="form-check-input me-2 js-license" type="radio" name="licenseType" value="${String.valueOf(lic.get("name"))}" data-price-cents="${String.valueOf(lic.get("priceCents"))}" data-license-id="${String.valueOf(lic.get("id"))}" required>
              @endif
              ${msg.get("clubRegister.membershipOptions.license" + String.valueOf(lic.get("name")))}
            </span>
            <span class="text-body-secondary">${String.format(java.util.Locale.FRANCE, "%.2f", lic.get("priceCents") instanceof Number ? ((Number) lic.get("priceCents")).doubleValue() / 100.0 : 0.0)} €</span>
          </label>
          @endfor
        </div>

        @if((options != null && !options.isEmpty()))
          <div class="mb-2 fw-semibold">${msg.get("clubRegister.membershipOptions.optionsTitle")}</div>
          <div class="list-group mb-3" id="options-container">
            @for(var opt : options)
              @if(!((opt.get("accessRule") != null && "YOUNG_ONLY".equals(String.valueOf(opt.get("accessRule"))) && !playerYoung) || (opt.get("accessRule") != null && "NON_YOUNG_ONLY".equals(String.valueOf(opt.get("accessRule"))) && playerYoung) || (opt.get("accessRule") != null && "ADMIN".equals(String.valueOf(opt.get("accessRule"))))))
                <label class="list-group-item d-flex justify-content-between align-items-center" data-license-id="${String.valueOf(opt.get("licenseId"))}" data-option-id="${String.valueOf(opt.get("id"))}" data-option-type="${String.valueOf(opt.get("optionType"))}">
                  <span>
                    <input class="form-check-input me-2 js-option" type="checkbox" name="optionIds" value="${String.valueOf(opt.get("id"))}" data-price-cents="${String.valueOf(opt.get("amountCents"))}" data-option-type="${String.valueOf(opt.get("optionType"))}">
                    <strong>${msg.get("membershipOptionType." + String.valueOf(opt.get("optionType")))}</strong> - ${String.valueOf(opt.get("optionValue"))}
                  </span>
                  <span class="text-body-secondary">${String.format(java.util.Locale.FRANCE, "%.2f", opt.get("amountCents") instanceof Number ? ((Number) opt.get("amountCents")).doubleValue() / 100.0 : 0.0)} €</span>
                </label>
              @endif
            @endfor
          </div>
        @else
          <p class="mb-3">${msg.get("clubRegister.membershipOptions.none")}</p>
        @endif

        <div class="alert alert-info d-flex justify-content-between align-items-center" role="status">
          <span class="fw-semibold">${msg.get("clubRegister.membershipOptions.total")}</span>
          <span id="membership-total">${String.format(java.util.Locale.US, "%.2f", 0.0)} €</span>
        </div>
        <button type="submit" class="btn btn-primary">${msg.get("action.save")}</button>
      </form>
    </div>
  </main>

  <script>
    (function () {
      const totalElement = document.getElementById('membership-total');
      if (!totalElement) {
        return;
      }

      const licenseInputs = Array.from(document.querySelectorAll('.js-license'));
      const optionInputs = Array.from(document.querySelectorAll('.js-option'));
      const optionsContainer = document.getElementById('options-container');

      const readPrice = function (input) {
        const raw = input.getAttribute('data-price-cents') || '0';
        const normalized = raw.toString().replace(/[^0-9.-]/g, '');
        const parsed = Number(normalized);
        return Number.isFinite(parsed) ? parsed : 0;
      };

      const formatter = new Intl.NumberFormat(document.documentElement.lang || navigator.language, {
        style: 'currency',
        currency: 'EUR'
      });

      const updateOptionVisibility = function () {
        if (!optionsContainer) {
          return;
        }
        const selectedLicense = licenseInputs.find(function (input) {
          return input.checked;
        });
        
        if (!selectedLicense) {
          return;
        }

        const selectedLicenseId = parseInt(selectedLicense.getAttribute('data-license-id') || '0', 10);

        // Update option disabled state
        const optionLabels = optionsContainer.querySelectorAll('label');
        optionLabels.forEach(function (label) {
          const optionLicenseId = parseInt(label.getAttribute('data-license-id') || '0', 10);
          const checkbox = label.querySelector('input[type="checkbox"]');
          
          // Option is enabled if:
          // - it applies to all licenses (optionLicenseId === 0) OR
          // - it matches the selected license (optionLicenseId === selectedLicenseId)
          const shouldEnable = optionLicenseId === 0 || optionLicenseId === selectedLicenseId;
          
          if (checkbox) {
            checkbox.disabled = !shouldEnable;
            
            // If option becomes disabled and is checked, uncheck it
            if (!shouldEnable && checkbox.checked) {
              checkbox.checked = false;
            }
          }
          
          // Add/remove visual disabled state
          if (!shouldEnable) {
            label.classList.add('opacity-75');
          } else {
            label.classList.remove('opacity-75');
          }
        });
      };

      const render = function () {
        let totalCents = 0;

        const selectedLicense = licenseInputs.find(function (input) {
          return input.checked;
        });
        if (selectedLicense) {
          totalCents += readPrice(selectedLicense);
        }

        optionInputs.forEach(function (input) {
          if (input.disabled && input.checked) {
            input.checked = false;
          }
          if (input.checked) {
            totalCents += readPrice(input);
          }
        });

        totalElement.textContent = formatter.format(totalCents / 100);
      };

      // Handle COURSES option exclusivity
      optionInputs.forEach(function (input) {
        input.addEventListener('change', function () {
          // If this is a COURSES checkbox and it's being checked
          if (this.checked && this.getAttribute('data-option-type') === 'COURSES') {
            // Uncheck all other COURSES checkboxes
            optionInputs.forEach(function (otherInput) {
              if (otherInput !== input && otherInput.getAttribute('data-option-type') === 'COURSES' && otherInput.checked) {
                otherInput.checked = false;
              }
            });
          }
          updateOptionVisibility();
          render();
        });
      });

      licenseInputs.forEach(function (input) {
        input.addEventListener('change', function () {
          updateOptionVisibility();
          render();
        });
      });

      updateOptionVisibility();
      render();
    })();
  </script>

  @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>