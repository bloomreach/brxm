<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.FaqDocument" -->
<#if document??>
<div class="has-edit-button">
<@hst.cmseditlink hippobean=document/>
<h1>${document.question}</h1>
<@hst.html hippohtml=document.answer />
</div>
</#if>


