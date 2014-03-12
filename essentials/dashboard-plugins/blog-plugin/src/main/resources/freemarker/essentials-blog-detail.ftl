<#include "/WEB-INF/ftl/essentials/common/imports.ftl">
<#-- @ftlvariable name="document" type="{{beansPackage}}.Blogpost" -->
<#if document??>
<h1>${document.title}</h1>
<h2>by:${document.author}</h2>
<strong>
    <#if document.publicationDate??>
        <@fmt.formatDate type="date" pattern="yyyy-MM-dd" value=document.publicationDate.time/>
    </#if>
</strong>
<p>${document.introduction}</p>
<div>
    <@hst.html hippohtml=document.content />
</div>
</#if>