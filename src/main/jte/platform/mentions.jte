@param String contextPath
@param String i18ntitle
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param com.github.gcolin.membership.License license
@param Object p
@param com.github.gcolin.payment.Payment payment
@param java.util.List<String> users
!{i18ntitle = msg.get("legal.title");}
!{title = i18ntitle;}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<body class="d-flex flex-column min-vh-100">
!{page = "home";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">

    <div class="container my-5">
    <h1 class="mb-4">${msg.get("legal.title")}</h1>

    <section class="mb-4">
        <h3>${msg.get("legal.publisher.title")}</h3>
        <p>
            ${msg.get("legal.publisher.text", pageConfig.getOrgName() != null ? pageConfig.getOrgName() : "", pageConfig.getOrgAddress() != null ? pageConfig.getOrgAddress() : "")}
        </p>
        <p>${msg.get("legal.contactEmail")} @if((pageConfig.getOrgEmail() != null && !pageConfig.getOrgEmail().isEmpty()))<a href="mailto:${pageConfig.getOrgEmail()}">${pageConfig.getOrgEmail()}</a>@endif</p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.hosting.title")}</h3>
        <p>
            ${msg.get("legal.hosting.text", pageConfig.getOrgName() != null ? pageConfig.getOrgName() : "", pageConfig.getOrgHostingAddress() != null ? pageConfig.getOrgHostingAddress() : "")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.purpose.title")}</h3>
        <p>
            ${msg.get("legal.purpose.text", pageConfig.getOrgName() != null ? pageConfig.getOrgName() : "")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.payment.title")}</h3>
        <p>
            ${msg.get("legal.payment.online")}
        </p>

        <p>
            ${msg.get("legal.payment.transfer")}
        </p>

        <p>
            ${msg.get("legal.payment.dispute")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.users.title")}</h3>
        <p>
            ${msg.get("legal.users.google")}
        </p>
        <p>
            ${msg.get("legal.users.auth")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.data.title")}</h3>
        <p>
            ${msg.get("legal.data.collected")}
        </p>
        <ul>
            <li>${msg.get("legal.data.item.license")}</li>
            <li>${msg.get("legal.data.item.email")}</li>
            <li>${msg.get("legal.data.item.registration")}</li>
        </ul>
        <p>
            ${msg.get("legal.data.source")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.cookies.title")}</h3>
        <p>
            ${msg.get("legal.cookies.text")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.rights.title")}</h3>
        <p>
            ${msg.get("legal.rights.text")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.ip.title")}</h3>
        <p>
            ${msg.get("legal.ip.text", pageConfig.getOrgName() != null ? pageConfig.getOrgName() : "")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.liability.title")}</h3>
        <p>
            ${msg.get("legal.liability.text", pageConfig.getOrgName() != null ? pageConfig.getOrgName() : "")}
        </p>
    </section>

    <section class="mb-4">
        <h3>${msg.get("legal.software.title")}</h3>
        <p>${msg.get("legal.software.text")}</p>
        @if(pageConfig.getSourceUrl() != null && !pageConfig.getSourceUrl().isBlank())
          <p>
            <a href="${pageConfig.getSourceUrl()}" target="_blank" rel="noopener noreferrer">${msg.get("legal.software.link")}</a>
            —
            <a href="https://www.gnu.org/licenses/agpl-3.0.html" target="_blank" rel="noopener noreferrer">${msg.get("footer.license")}</a>
          </p>
        @endif
    </section>
</div>

    </main>

    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
</body>

</html>