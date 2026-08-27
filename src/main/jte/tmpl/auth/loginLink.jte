@param com.github.gcolin.auth.LoginMail mail
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px; }
    .container { max-width: 600px; margin: auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
    .header { background: #1f2937; color: #ffffff; padding: 20px; text-align: center; }
    .content { padding: 25px; color: #333333; }
    a.button { display: inline-block; background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 5px; margin-top: 12px; }
    .footer { font-size: 12px; color: #777777; text-align: center; padding: 15px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>Lien de connexion</h1>
    </div>
    <div class="content">
      <p>Bonjour <strong>${mail.getName()}</strong>,</p>
      <p>Utilisez le lien ci-dessous pour vous connecter :</p>
      <p><a class="button" href="$unsafe{mail.getLoginUrl()}">Se connecter</a></p>
      <p>Ce lien est valable 30 jours.</p>
      <p>A bientot,<br>
      <strong>${mail.getOrgName() != null ? mail.getOrgName() : ""}</strong></p>
    </div>
    <div class="footer">
      Cet email est envoye automatiquement, merci de ne pas repondre.
    </div>
  </div>
</body>
</html>
