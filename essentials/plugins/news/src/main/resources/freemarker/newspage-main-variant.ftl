<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.NewsDocument" -->
<#if document??>
  <@hst.link var="link" hippobean=document/>
<article class="has-edit-button">
  <@hst.manageContent hippobean=document/>
  <h3><a href="${link}">${document.title?html}</a>
    <#if document.date??>
      <small><@fmt.formatDate value=document.date.time type="date" dateStyle="medium"/></small>
    </#if>
  </h3>
  <#if document.introduction??>
    <p class="lead">${document.introduction?html}</p>
  </#if>
  <@hst.html hippohtml=document.content/>
  <#if document.author??>
    <footer>
      <p>-- by <em>${document.author?html}</em></p>
    </footer>
  </#if>
</article>
</#if>