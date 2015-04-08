<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="org.example.beans.ContentDocument" -->
<#if document??>
    <@hst.link var="link" hippobean=document/>
    <article class="has-edit-button">
        <@hst.cmseditlink hippobean=document/>
        <h3>
            <a href="${link}">${document.title?html}</a>
            <#if document.publicationDate??>
                <small><@fmt.formatDate value=document.publicationDate.time type="date" dateStyle="medium"/></small>
            </#if>
        </h3>
        <#if document.introduction??>
            <p class="lead">${document.introduction?html}</p>
        </#if>
        <@hst.html hippohtml=document.content/>
    </article>
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
    <img src="<@hst.link path="/images/essentials/catalog-component-icons/simple-content.png" />"> Click to edit Simple Content
</#if>