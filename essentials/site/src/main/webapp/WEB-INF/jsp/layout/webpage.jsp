<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <hst:headContributions categoryExcludes="scripts" xhtml="true"/>
  <hst:link var="link" path="/css/style.css"/>
  <link rel="stylesheet" href="${link}" type="text/css"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
</head>
<body>
<hst:include ref="header"/>

<hst:include ref="main"/>

<hst:include ref="footer"/>

<hst:include ref="sitetab"/>

<hst:include ref="codeBottom"/>

<hst:headContributions categoryIncludes="scripts" xhtml="true"/>
</body>
</html>
