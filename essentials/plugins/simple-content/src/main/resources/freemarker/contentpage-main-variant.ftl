<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.ContentDocument" -->
<#if document??>
<article class="has-edit-button">
  <@hst.manageContent hippobean=document/>
  <h3>${document.title?html}
    <#if document.publicationDate??>
      <small><@fmt.formatDate value=document.publicationDate.time type="date" dateStyle="medium"/></small>
    </#if>
  </h3>
  <#if document.introduction??>
    <p class="lead">${document.introduction?html}</p>
  </#if>
  <@hst.html hippohtml=document.content/>
</article>
</#if>