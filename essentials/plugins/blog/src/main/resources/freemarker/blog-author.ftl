<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Author" -->
<@hst.defineObjects/>
<#if document??>
<h1>
${document.fullName}
</h1>
</#if>