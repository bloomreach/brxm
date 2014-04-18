<#include "/WEB-INF/freemarker/include/imports.ftl">
<#-- @ftlvariable name="query" type="java.lang.String" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable??>
    <#if pageable?? && pageable.total ==0>
    <h3>No results for: ${query}</h3>
    <#else>
        <#list pageable.items as item>
            <@hst.link var="link" hippobean=item />
        <article>
            <h3><a href="${link}">${item.title}</a></h3>
        </article>
        </#list>
        <#if pageable.showPagination??>
            <#include "/WEB-INF/freemarker/include/pagination.ftl">
        </#if>
    </#if>
</#if>