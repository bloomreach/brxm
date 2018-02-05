<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.ContentDocument" -->
<#if document??>
  <article class="has-edit-button">
    <@hst.manageContent hippobean=document />
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
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <div class="has-edit-button">
    <img src="<@hst.link path="/images/essentials/catalog-component-icons/simple-content.png" />"> Click to edit Simple Content
    <@hst.manageContent templateQuery="new-content-document" rootPath="content"/>
  </div>
</#if>