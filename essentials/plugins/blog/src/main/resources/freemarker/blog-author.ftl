<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Author" -->
<@hst.defineObjects/>
<#if document??>
<h1>
${document.fullName?html}
</h1>
</#if>