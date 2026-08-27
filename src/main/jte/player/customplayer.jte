@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.player.CustomPlayer player
@param Integer eventId
!{i18ntitle = msg.get("player.title");}
!{title = i18ntitle + " " + (player.getName() != null ? player.getName() : "");}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "sub";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
    <div class="bg-body p-3 rounded mb-3">
    <div class="row justify-content-center mt-3">
        <form id="editForm" method="post" action="${contextPath}/customplayer">

            <input type="hidden" name="id" value="${player.getId() != null ? String.valueOf(player.getId()) : ""}">

            <input type="hidden" name="eventId" id="eventId" value="${eventId != null ? String.valueOf(eventId) : ""}">

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("player.licence")}
                </label>
                <input type="text" id="licence" name="licence" value="${player.getLicence() != null ? player.getLicence() : ""}" class="form-control" required>
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("player.familyname")}
                </label>
                <input type="text" id="name" name="name" value="${player.getName() != null ? player.getName() : ""}" class="form-control" required>
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("player.firstname")}
                </label>
                <input type="text" id="firstname" name="firstname" value="${player.getFirstname() != null ? player.getFirstname() : ""}" class="form-control" required>
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("player.birthdate")}
                </label>
                <input type="date" id="birthdate" name="birthdate" value="${player.getBirthDate() != null ? player.getBirthDate() : ""}" class="form-control" required>
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("player.rating")}
                </label>
                <input type="text" id="elo" name="elo" value="${player.getElo() != null ? player.getElo() : ""}" class="form-control" required>
            </div>

            <div class="mb-3">
                <label class="form-label">
                    ${msg.get("player.gender")}
                </label>
                <div class="form-check">
                    @if(player.getGender() != null && player.getGender())
                    <input type="radio" id="genderMale" name="gender" value="true" class="form-check-input" checked>
                    @else
                    <input type="radio" id="genderMale" name="gender" value="true" class="form-check-input">
                    @endif
                    <label class="form-check-label" for="genderMale">
                        ${msg.get("player.male")}
                    </label>
                </div>
                <div class="form-check">
                    @if(player.getGender() != null && !player.getGender())
                    <input type="radio" id="genderFemale" name="gender" value="false" class="form-check-input" checked>
                    @else
                    <input type="radio" id="genderFemale" name="gender" value="false" class="form-check-input">
                    @endif
                    <label class="form-check-label" for="genderFemale">
                        ${msg.get("player.female")}
                    </label>
                </div>
            </div>

            @if(user.hasPermission("EVENT_EDIT"))
                <div class="mb-3">
                <label class="form-label">
                    creationUser
                </label>
                <input type="text" id="creationUser" name="creationUser" value="${player.getCreationUser() != null ? player.getCreationUser() : ""}" class="form-control">
                </div>
            @endif

            <div class="d-flex justify-content-between">

                <button type="submit" id="save" class="btn btn-outline-primary">
                    💾 ${msg.get("action.save")}
                </button>
            </div>

        </form>
    </div></div>
    </main>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>
</html>
