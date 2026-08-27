@param String title
@param String contextPath
@param com.github.gcolin.platform.Page pageConfig
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="robots" content="none">
<title>${title != null ? title : ""}</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<script type="text/javascript"
	src="${contextPath}/js/bootstrap.bundle.min.js"></script>
<link rel="stylesheet" type="text/css"
	href="${contextPath}/css/theme.css" />
<link rel="stylesheet" type="text/css"
    href="${contextPath}/css/bootstrap-icons.min.css" />
<link rel="icon" type="image/x-icon"
	href="${contextPath}/favicon.ico" />

<style type="text/css">
        .bg-chess-overlay {
          background: url('${contextPath}${pageConfig.getBackgroundUrl() != null && !pageConfig.getBackgroundUrl().isBlank() ? pageConfig.getBackgroundUrl() : "/assets/6.jpg"}') center / cover no-repeat fixed;
        }
      </style>
</head>
