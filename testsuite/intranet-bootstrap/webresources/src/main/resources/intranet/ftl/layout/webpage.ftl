<!DOCTYPE html>
<#include "../htmlTags.ftl">
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <@hst.headContributions categoryExcludes="scripts" xhtml=true/>

    <@hst.webresource var="link" path="/css/style.css"/>
    <link rel="stylesheet" href="${link}" type="text/css"/>

    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
</head>
<body>

<@hst.include ref="header"/>
<@hst.include ref="main"/>
<@hst.headContributions categoryIncludes="scripts" xhtml=true/>
</body>
</html>
