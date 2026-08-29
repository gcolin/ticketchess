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
@param Object message
@param Object p
@param String status
@param Object type

!{i18ntitle = msg.get("page.database");}
!{title = i18ntitle;}
@template.platform.adminLayout(
  title=title,
  contextPath=contextPath,
  pageConfig=pageConfig,
  msg=msg,
  notifications=notifications,
  state=state,
  user=user,
  activePageTitle=msg.get("page.database"),
  content = @`
    @template.platform.adminPageHeader(title=msg.get("heading.databaseAdmin"), helpText=msg.get("help.databaseDesc"), icon="bi-database")

    @template.platform.adminPanel(
      heading=msg.get("label.ffeDatabase"),
      content = @`
        <pre id="ffe-info" class="flex-grow-1 bg-light p-2 rounded mb-0" style="overflow:auto"></pre>
        <div class="mt-3 d-flex justify-content-between">
          <button class="btn btn-primary" id="download-ffe">${msg.get("action.downloadExtract")}</button>
          <button class="btn  btn-secondary" id="refresh-ffe">${msg.get("action.refresh")}</button>
        </div>
      `
    )

    @template.platform.adminPanel(
      heading=msg.get("label.fideDatabase"),
      content = @`
        <pre id="fide-info" class="flex-grow-1 bg-light p-2 rounded mb-0" style="overflow:auto"></pre>
        <div class="mt-3 d-flex justify-content-between">
          <button class="btn btn-primary" id="download-fide">${msg.get("action.downloadExtract")}</button>
          <button class="btn  btn-secondary" id="refresh-fide">${msg.get("action.refresh")}</button>
        </div>
      `
    )

    @template.platform.adminPanel(content = @`
      <div class="text-center">
        <button class="btn btn-danger" id="reload-db">${msg.get("action.reloadDb")}</button>
      </div>
    `)

    <div id="status-alert" class="alert d-none" role="alert"></div>
  `,
  scripts = @`
    <script type="text/javascript">
      function setStatus(message, isError){
        const el=document.getElementById('status-alert');
        el.className='alert ' + (isError?'alert-danger':'alert-success');
        el.textContent=message;
        el.classList.remove('d-none');
      }
      async function fetchText(url){
        const res=await fetch(url, {headers:{'Accept':'text/plain'}});
        if(!res.ok) throw new Error(await res.text());
        return await res.text();
      }
      async function loadDBInfo(){
        try{ document.getElementById('ffe-info').textContent=await fetchText('${contextPath}/database/ffe'); }
        catch(e){ document.getElementById('ffe-info').textContent='${msg.get("error.prefix")}'+e.message; }
        try{ document.getElementById('fide-info').textContent=await fetchText('${contextPath}/database/fide'); }
        catch(e){ document.getElementById('fide-info').textContent='${msg.get("error.prefix")}'+e.message; }
      }
      document.getElementById('download-ffe').addEventListener('click', async () => {
        const btn = document.getElementById('download-ffe');
        btn.disabled = true;
        const originalText = btn.textContent;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> ${msg.get("action.downloadExtract")}';
        try{ await fetchText('${contextPath}/database/ffe/download'); setStatus('${msg.get("database.downloadFfeOk")}'); }
        catch(e){ setStatus(e.message, true); }
        loadDBInfo();
        btn.disabled = false;
        btn.textContent = originalText;
      });

      document.getElementById('download-fide').addEventListener('click', async () => {
        const btn = document.getElementById('download-fide');
        btn.disabled = true;
        const originalText = btn.textContent;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> ${msg.get("action.downloadExtract")}';
        try{ await fetchText('${contextPath}/database/fide/download'); setStatus('${msg.get("database.downloadFideOk")}'); }
        catch(e){ setStatus(e.message, true); }
        loadDBInfo();
        btn.disabled = false;
        btn.textContent = originalText;
      });

      document.getElementById('reload-db').addEventListener('click', async () => {
        const btn = document.getElementById('reload-db');
        btn.disabled = true;
        const originalText = btn.textContent;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> ${msg.get("database.reloading")}';
        try{ await fetchText('${contextPath}/database/reload'); setStatus('${msg.get("database.reloadOk")}'); }
        catch(e){ setStatus(e.message, true); }
        loadDBInfo();
        btn.disabled = false;
        btn.textContent = originalText;
      });
      document.getElementById('refresh-ffe').addEventListener('click', loadDBInfo);
      document.getElementById('refresh-fide').addEventListener('click', loadDBInfo);
      loadDBInfo();
    </script>
  `
)
