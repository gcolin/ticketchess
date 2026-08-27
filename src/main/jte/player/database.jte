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
!{page = "database";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<html>
  <body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "database";}
    @template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
    <main class="container flex-fill">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb bg-body rounded px-3 py-2 mb-0">
          <li class="breadcrumb-item"><a href="${contextPath}/">${msg.get("breadcrumb.home")}</a></li>
          <li class="breadcrumb-item"><a href="${contextPath}/admin">${msg.get("menu.admin")}</a></li>
          <li class="breadcrumb-item active" aria-current="page">${msg.get("page.database")}</li>
        </ol>
      </nav>
      <div class="row g-4">
        <div class="col-md-12">
        <div class="card">
        <div class="card-body d-flex flex-column">
          <h1>${msg.get("heading.databaseAdmin")}</h1>
          <p class="lead">${msg.get("help.databaseDesc")}</p>
        </div>
        </div>
        </div>
        <div class="col-md-12">
          <div class="card">
            <div class="card-header">${msg.get("label.ffeDatabase")}</div>
            <div class="card-body d-flex flex-column">
              <pre id="ffe-info" class="flex-grow-1 bg-light p-2 rounded" style="overflow:auto"></pre>
              <div class="mt-3 d-flex justify-content-between">
                <button class="btn btn-primary" id="download-ffe">${msg.get("action.downloadExtract")}</button>
                <button class="btn btn-secondary" id="refresh-ffe">${msg.get("action.refresh")}</button>
              </div>
            </div>
          </div>
        </div>
        <div class="col-md-12">
          <div class="card">
            <div class="card-header">${msg.get("label.fideDatabase")}</div>
            <div class="card-body d-flex flex-column">
              <pre id="fide-info" class="flex-grow-1 bg-light p-2 rounded" style="overflow:auto"></pre>
              <div class="mt-3 d-flex justify-content-between">
                <button class="btn btn-primary" id="download-fide">${msg.get("action.downloadExtract")}</button>
                <button class="btn btn-secondary" id="refresh-fide">${msg.get("action.refresh")}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="row mt-4">
        <div class="col-12 text-center">
          <button class="btn btn-danger" id="reload-db">${msg.get("action.reloadDb")}</button>
        </div>
      </div>
      <div class="row mt-3">
        <div class="col-12">
          <div id="status-alert" class="alert d-none" role="alert"></div>
        </div>
      </div>
    </main>
    @template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)
<script type="text/javascript">
// Helper to set status alert
function setStatus(message, isError){
  const el=document.getElementById('status-alert');
  el.className='alert ' + (isError?'alert-danger':'alert-success');
  el.textContent=message;
  el.classList.remove('d-none');
}
// Fetch JSON/text from endpoint
async function fetchText(url){
  const res=await fetch(url, {headers:{'Accept':'text/plain'}});
  if(!res.ok) throw new Error(await res.text());
  return await res.text();
}
// Load info for both DBs
async function loadDBInfo(){
  try{ document.getElementById('ffe-info').textContent=await fetchText('${contextPath}/database/ffe'); }
  catch(e){ document.getElementById('ffe-info').textContent='${msg.get("error.prefix")}'+e.message; }
  try{ document.getElementById('fide-info').textContent=await fetchText('${contextPath}/database/fide'); }
  catch(e){ document.getElementById('fide-info').textContent='${msg.get("error.prefix")}'+e.message; }
}
// Download & extract buttons
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
// Refresh info buttons
document.getElementById('refresh-ffe').addEventListener('click', loadDBInfo);
document.getElementById('refresh-fide').addEventListener('click', loadDBInfo);
// Initial load
loadDBInfo();
</script>
  </body>
</html>