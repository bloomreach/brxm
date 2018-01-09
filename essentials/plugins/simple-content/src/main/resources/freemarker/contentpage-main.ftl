<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.ContentDocument" -->
<#if document??>
<article class="has-edit-button">
  <@hst.manageContent document=document />
  <h3>${document.title?html}</h3>
  <#if document.publicationDate??>
    <p>
      <@fmt.formatDate value=document.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </#if>
  <#if document.introduction??>
    <p>
    ${document.introduction?html}
    </p>
  </#if>
  <@hst.html hippohtml=document.content/>
</article>
</#if>