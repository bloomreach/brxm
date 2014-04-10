<#include "/WEB-INF/freemarker/include/imports.ftl">
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="item" type="{{beansPackage}}.ContentDocument" -->
<#-- @ftlvariable name="showPagination" type="java.lang.Boolean" -->
<#if pageable??>
    <#list pageable.items as item>
        <@hst.link var="link" hippobean=item/>
    <article>
        <@hst.cmseditlink hippobean=item/>
        <h3><a href="${link}">
        ${item.title}
        </a></h3>
        <#if item.publicationdate??>
            <p>
                <@fmt.formatDate value=item.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/>
            </p>
        </#if>
        <p>
        ${item.introduction}
        </p>
    </article>
    </#list>
</#if>
<#if showPagination>
    <#include "/WEB-INF/freemarker/include/pagination.ftl">
</#if>