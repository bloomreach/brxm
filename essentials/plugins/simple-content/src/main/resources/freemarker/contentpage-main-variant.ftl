<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.ContentDocument" -->
<#if document??>
<article class="has-edit-button">
  <@hst.manageContent templateQuery="new-content-document" document=document defaultPath="content"/>
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
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <img src="<@hst.link path="/images/essentials/catalog-component-icons/simple-content.png" />"> Click to edit Simple Content
  <@hst.manageContent templateQuery="new-content-document" defaultPath="content"/>
</div>
</#if>