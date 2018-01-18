<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.FaqItem" -->
<#if document??>
<div class="has-edit-button">
  <@hst.manageContent templateQuery="new-faq-item" document=document rootPath="faq"/>
  <h1>${document.question?html}</h1>
  <@hst.html hippohtml=document.answer />
</div>
</#if>


