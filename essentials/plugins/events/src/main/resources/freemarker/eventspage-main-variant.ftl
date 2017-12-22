<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.EventsDocument" -->
<#if document??>
    <@hst.link var="link" hippobean=document/>
<article class="has-edit-button">
    <@hst.manageContent templateQuery="new-events-document" document=document defaultPath="events"/>
  <h3><a href="${link}">${document.title?html}</a>
      <#if document.date??>
        <small><@fmt.formatDate value=document.date.time type="date" dateStyle="medium"/></small>
      </#if>
  </h3>
    <#if document.location??>
      <p>${document.location?html}</p>
    </#if>
    <#if document.introduction??>
      <p class="lead">${document.introduction?html}</p>
    </#if>
    <@hst.html hippohtml=document.content/>
</article>
</#if>