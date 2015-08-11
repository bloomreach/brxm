<#include "../include/imports.ftl">

<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="item" type="{{beansPackage}}.ContentDocument" -->
<#if pageable?? && pageable.items?has_content>
    <#list pageable.items as item>
        <article class="has-edit-button">
            <@hst.cmseditlink hippobean=item/>
            <@hst.link var="link" hippobean=item/>
            <h3><a href="${link}">${item.title?html}</a></h3>
            <#if item.publicationDate??>
                <p>
                    <@fmt.formatDate value=item.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/>
                </p>
            </#if>
            <#if item.introduction??>
                <p>${item.introduction?html}</p>
            </#if>
        </article>
    </#list>
    <#if cparam.showPagination>
        <#include "../include/pagination.ftl">
    </#if>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/generic-list.png'/>"> Click to edit list
</#if>
