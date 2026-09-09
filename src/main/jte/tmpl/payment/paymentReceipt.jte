@param com.github.gcolin.payment.PaymentReceiptMail mail
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px; }
    .container { max-width: 600px; margin: auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
    .header { background: #0f766e; color: #ffffff; padding: 20px; text-align: center; }
    .content { padding: 25px; color: #333333; }
    .box { background: #f0fdfa; border-left: 4px solid #0f766e; padding: 15px; margin: 20px 0; }
    .box p { margin: 6px 0; }
    .footer { font-size: 12px; color: #777777; text-align: center; padding: 15px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>${mail.isDonation() ? "Attestation fiscale" : "Reçu de paiement"}</h1>
    </div>
    <div class="content">
      <p>Bonjour <strong>${mail.getName() != null ? mail.getName() : ""}</strong>,</p>
      <p>
        @if(mail.isDonation())
          Veuillez trouver ci-joint votre attestation de don.
        @else
          Veuillez trouver ci-joint votre reçu de paiement.
        @endif
      </p>
      <div class="box">
        <p><strong>${mail.getDocumentLabel() != null ? mail.getDocumentLabel() : ""}</strong></p>
        @if(mail.getAmount() != null && !mail.getAmount().isBlank())
          <p>💶 Montant : <strong>${mail.getAmount()}</strong></p>
        @endif
        @if(mail.getReference() != null && !mail.getReference().isBlank())
          <p>🧾 Référence : ${mail.getReference()}</p>
        @endif
      </div>
      <p>Merci pour votre confiance,<br>
      <strong>${mail.getOrgName() != null ? mail.getOrgName() : ""}</strong></p>
    </div>
    <div class="footer">
      Cet email est envoyé automatiquement, merci de ne pas répondre.
    </div>
  </div>
</body>
</html>
