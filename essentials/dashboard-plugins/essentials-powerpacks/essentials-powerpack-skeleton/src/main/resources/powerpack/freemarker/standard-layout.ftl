<!doctype html>
<#include "/WEB-INF/freemarker/include/imports.ftl">
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <link rel="stylesheet" href="<@hst.link  path="/css/bootstrap.css"/>" type="text/css"/>
  <@hst.headContributions categoryIncludes="componentsCss" xhtml="true"/>
  <@hst.headContributions categoryIncludes="globalJavascript" xhtml="true"/>
</head>
<body>
<div class="container">
  <div class="row">
    <div class="col-md-6 col-md-offset-3"><@hst.include ref="top"/></div>
  </div>
  <div class="row">
    <@hst.include ref="main"/>
  </div>
</div>
<@hst.headContributions categoryIncludes="componentsJavascript" xhtml="true"/>
</body>
</html>