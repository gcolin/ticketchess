@param com.github.gcolin.platform.BroadcastMail mail
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px; }
    .container { max-width: 600px; margin: auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
    .header { background: #1f2937; color: #ffffff; padding: 20px; text-align: center; }
    .content { padding: 25px; color: #333333; }
    .footer { font-size: 12px; color: #777777; text-align: center; padding: 15px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header" style="background: ${mail.getHeaderBackgroundColor() != null && !mail.getHeaderBackgroundColor().isBlank() ? mail.getHeaderBackgroundColor() : "#1f2937"}; color: ${mail.getHeaderTextColor() != null && !mail.getHeaderTextColor().isBlank() ? mail.getHeaderTextColor() : "#ffffff"};">
      <h1>@if(mail.getHeaderIcon() != null && !mail.getHeaderIcon().isBlank())${mail.getHeaderIcon()} @endif${mail.getEventName()}@if(mail.getName() != null && !mail.getName().isBlank()) - ${mail.getName()}@endif</h1>
    </div>
    <div class="content">
      $unsafe{mail.getBody()}
      @if(mail.getLoginUrl() != null && !mail.getLoginUrl().isBlank())
      <p><a class="button" href="$unsafe{mail.getLoginUrl()}" style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 10px 20px; border-radius: 5px; text-decoration: none;">${mail.getButtonText() != null && !mail.getButtonText().isBlank() ? mail.getButtonText() : "Accéder au site"}</a></p>
      @endif
    </div>
    <div class="footer">
      <p>${mail.getOrgName() != null ? mail.getOrgName() : ""}</p>
    </div>
  </div>
</body>
</html>
