<#include "../include/imports.ftl">

<#-- @ftlvariable name="item" type="com.onehippo.cms7.personalization.Category" -->
<#-- @ftlvariable name="category" type="com.onehippo.cms7.personalization.Category" -->
<#-- @ftlvariable name="pageable" type="com.onehippo.cms7.personalization.BloomreachCategoryPageable" -->
<#if pageable?? && pageable.items?has_content>
    <#list pageable.items as item>
  <div class="category-wrapper widget-categories">
      <div class="title">${item.name}</div>
    <#list item.subCategories as category>
        <#if category.selected>
        <a class="category" href="${category.removeUrl}" data-count="${category.total}">
          <span style="color: red">X
        </span>${category.name}</a>
        <#else>
        <a class="category" href="${category.url}" data-count="${category.total}">${category.name}</a>
        </#if>
    </#list>
  </div>
    </#list>
    <@hst.headContribution category="htmlHead">
  <link rel="stylesheet" href="<@hst.webfile  path="/css/categories.css"/>" type="text/css"/>
    </@hst.headContribution>

<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/commerce-search-category-list.png'/>"> Click to
    edit category list
</div>
</#if>



