@param com.github.gcolin.payment.PaymentMail mail
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px; }
    .container { max-width: 600px; margin: auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
    .header { background: #16a34a; color: #ffffff; padding: 20px; text-align: center; }
    .content { padding: 25px; color: #333333; }
    .payment { background: #f0fdf4; border-left: 4px solid #16a34a; padding: 15px; margin: 20px 0; }
    .payment p { margin: 6px 0; }
    .footer { font-size: 12px; color: #777777; text-align: center; padding: 15px; }
    a.button { display: inline-block; background-color: #16a34a; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 5px; margin-top: 10px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>Paiement confirmé ✅</h1>
    </div>
    <div class="content">
      <p>Bonjour <strong>${mail.getName()}</strong>,</p>
      <p>Nous confirmons la bonne réception de votre paiement pour l'événement :</p>
      <div class="payment">
        <p><strong>${mail.getEventName()}</strong></p>
        @if(mail.getAmount() != null && !mail.getAmount().isBlank())
        <p>💶 Montant : <strong>${mail.getAmount()}</strong></p>
        @endif
        <p>🧾 Référence : ${mail.getReference()}</p>
        <p>📅 Date : ${mail.getEvenDate()}</p>
      </div>
      <p>Votre inscription est désormais <strong>validée</strong>.</p>
      @if(mail.getLoginUrl() != null && !mail.getLoginUrl().isBlank())
      <p><a class="button" href="$unsafe{mail.getLoginUrl()}">Voir mon événement</a></p>
      @endif
      <p>Merci pour votre confiance et à très bientôt,<br>
      <strong>${mail.getOrgName() != null ? mail.getOrgName() : ""}</strong></p>
    </div>
    <div class="footer">
      Cet email est envoyé automatiquement, merci de ne pas répondre.
    </div>
  </div>
</body>
</html>
