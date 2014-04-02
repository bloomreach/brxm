<#include "/WEB-INF/ftl/essentials/common/imports.ftl">
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable??>
    <#list pageable.items as item>
        <@hst.link var="link" hippobean=item />
    <article>
        <h3><a href="${link}">${item.title}</a></h3>
    </article>
    </#list>
    <#if showPagination??>
        <#include "/WEB-INF/ftl/essentials/common/pagination.ftl">
    </#if>
</#if>