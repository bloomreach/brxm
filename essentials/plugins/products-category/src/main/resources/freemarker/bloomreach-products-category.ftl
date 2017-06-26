<#include "../include/imports.ftl">

<#-- @ftlvariable name="item" type="com.onehippo.cms7.personalization.Category" -->
<#-- @ftlvariable name="category" type="com.onehippo.cms7.personalization.Category" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>

    <#list pageable.items as item>
    <div class="category-wrapper widget-categories">
      <div class="title">${item.name}</div>
        <#list item.subCategories as category>
          <a class="category" href="${category.url}" data-count="${category.total}">${category.name}</a>
        </#list>
    </div>
    </#list>
    <@hst.headContribution category="htmlHead">
    <link rel="stylesheet" href="<@hst.webfile  path="/css/categories.css"/>" type="text/css"/>
    </@hst.headContribution>

<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/bloomreach-products-category.png'/>"> Click to edit Product categories
</div>
</#if>



