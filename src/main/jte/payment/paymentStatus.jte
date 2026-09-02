@param String contextPath
@param com.github.gcolin.platform.Page pageConfig
@param String success
@param String title
@param Object p
@param com.github.gcolin.payment.Payment payment
@param String status
@param String simulationPath
<!DOCTYPE html>
<html lang="fr">
!{title = "Paiement";}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<body>
    <main class="d-flex justify-content-center align-items-center min-vh-100 bg-light">
        <div class="card shadow p-5 text-center">
            <h1 class="mb-4">Statut du Paiement</h1>
            <div class="gap-3 d-flex justify-content-center flex-wrap">
                <a class="btn btn-success btn-lg" href="${contextPath}${simulationPath}?status=paid">
                    ✓ Paiement Accepté
                </a>
                <a class="btn btn-danger btn-lg" href="${contextPath}${simulationPath}?status=rejected">
                    ✗ Paiement Refusé
                </a>
            </div>
        </div>
    </main>
</body>
</html>