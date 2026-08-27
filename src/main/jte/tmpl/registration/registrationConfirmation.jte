@param com.github.gcolin.registration.RegistrationMail mail
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px; }
    .container { max-width: 600px; margin: auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
    .header { background: #1f2937; color: #ffffff; padding: 20px; text-align: center; }
    .content { padding: 25px; color: #333333; }
    .event { background: #f9fafb; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0; }
    .footer { font-size: 12px; color: #777777; text-align: center; padding: 15px; }
    a.button { display: inline-block; background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 5px; margin-top: 10px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>Inscription confirmée ♟️</h1>
    </div>
    <div class="content">
      <p>Bonjour <strong>${mail.getName()}</strong>,</p>
      <p>Votre inscription à l'événement a bien été enregistrée :</p>
      <div class="event">
        <p><strong>${mail.getEventName()}</strong></p>
        <p>📅 ${mail.getEvenDate()}</p>
      </div>
      @if(mail.getAmount() != null && !mail.getAmount().isBlank())
      <div class="payment-reminder">
        <p>⚠️ N'oubliez pas de finaliser le paiement pour confirmer votre inscription :</p>
        <p>Montant dû : <strong>${mail.getAmount()}</strong></p>
      </div>
      @endif
      <p>
        <a class="button" href="$unsafe{mail.getLoginUrl()}">Voir mon événement</a>
      </p>
      <p>Nous avons hâte de vous retrouver !</p>
      <p>À très bientôt,<br>
      <strong>${mail.getOrgName() != null ? mail.getOrgName() : ""}</strong></p>
    </div>
    <div class="footer">
      Cet email est envoyé automatiquement, merci de ne pas répondre.
    </div>
  </div>
</body>
</html>
