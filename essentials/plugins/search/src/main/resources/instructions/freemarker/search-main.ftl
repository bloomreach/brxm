<#include "../include/imports.ftl">

<#-- @ftlvariable name="query" type="java.lang.String" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable??>
  <#if pageable.total == 0>
    <h3>No results for: ${query}</h3>
  <#else>
    <#list pageable.items as item>
      <@hst.link var="link" hippobean=item />
      <article>
        <h3><a href="${link}">${item.title}</a></h3>
      </article>
    </#list>
    <#if cparam.showPagination>
    <#include "../include/pagination.ftl">

    </#if>
  </#if>
<#else>
  <h3>Please fill in a search term.</h3>
</#if>