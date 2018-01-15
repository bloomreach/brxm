<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Blogpost" -->
<#if document??>
  <div class="has-edit-button">
    <@hst.manageContent document=document/>
    <h1>${document.title?html}</h1>
    <h2>by: ${document.author?html}</h2>
    <strong>
      <#if document.publicationDate??>
        <@fmt.formatDate type="date" pattern="yyyy-MM-dd" value=document.publicationDate.time/>
      </#if>
    </strong>
    <p>${document.introduction?html}</p>
    <div>
      <@hst.html hippohtml=document.content />
    </div>
  </div>
</#if>