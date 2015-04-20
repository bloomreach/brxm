<#include "../include/imports.ftl">

<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
  <#list pageable.items as item>
    <#if item.title??>
      <#assign linkName=item.title>
    <#else>
      <#assign linkName=item.localizedName>
    </#if>

    <article class="has-edit-button">
      <@hst.cmseditlink hippobean=item/>
      <@hst.link var="link" hippobean=item />
      <h3><a href="${link}">${linkName?html}</a></h3>
      <#if item.introduction??>
        <p>${item.introduction?html}</p>
      </#if>
    </article>
  </#list>
  <#if cparam.showPagination>
    <#include "../include/pagination.ftl">
  </#if>
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/generic-list.png'/>"> Click to edit Generic List
</#if>
