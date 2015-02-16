<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.ContentDocument" -->
<#if document??>
  <@hst.link var="link" hippobean=document/>
  <article class="has-edit-button">
    <@hst.cmseditlink hippobean=document/>
    <h3><a href="${link}">${document.title}</a></h3>
    <#if document.publicationDate??>
      <p>
        <@fmt.formatDate value=document.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/>
      </p>
    </#if>
    <#if document.introduction??>
      <p>
        ${document.introduction}
      </p>
    </#if>
    <@hst.html hippohtml=document.content/>
  </article>
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <img src="<@hst.link path="/images/essentials/catalog-component-icons/simple-content.png" />"> Click to edit Simple Content
</#if>