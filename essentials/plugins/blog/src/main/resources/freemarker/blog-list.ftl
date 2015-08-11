<#include "../../include/imports.ftl">

<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable??>
    <#list pageable.items as item>
        <@hst.link var="link" hippobean=item />
    <article class="has-edit-button">
        <@hst.cmseditlink hippobean=item/>
        <h3><a href="${link}">${item.title?html}</a></h3>
        <#if item.publicationDate?? && item.publicationDate.time??>
            <p><@fmt.formatDate value=item.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/></p>
        </#if>
        <p>${item.introduction?html}</p>
    </article>
    </#list>
    <#if cparam.showPagination>

    <#include "../../include/pagination.ftl">

    </#if>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/blog-list.png'/>"> Click to edit Blog List
</#if>