<#include "/WEB-INF/ftl/essentials/common/imports.ftl">
<#-- @ftlvariable name="document" type="{{beansPackage}}.ContentDocument" -->
<#if document??>
    <@hst.link var="link" hippobean=document/>
<article>
    <@hst.cmseditlink hippobean=document/>
    <h3><a href="${link}">${document.title}</a></h3>
    <#if item.publicationdate??>
        <p>
            <@fmt.formatDate value=item.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/>
        </p>
    </#if>
    <#if document.introduction??>
        <p>
        ${document.introduction}
        </p>
    </#if>
    <@hst.html hippohtml=document.content/>
</article>
</#if>