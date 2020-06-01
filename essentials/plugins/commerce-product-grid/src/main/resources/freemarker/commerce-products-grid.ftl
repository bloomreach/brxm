<#include "../include/imports.ftl">

<#-- @ftlvariable name="item" type="com.onehippo.cms7.personalization.Product" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
<div>
    <#list pageable.items as item>
      <div class="thumbnail" style="height: 300px; min-height: 300px;">
          <#if item.image??>
            <img src="${item.image}" width="150" height="150"/>
          </#if>
        <div class="caption center-block">
          <h3><p>${item.title}</p></h3>
        <#--<div>${item.description}</div>-->
          <p class="pull-right">
            <a class="btn btn-primary" href="${item.url}">${item.price}</a>
          </p>
        </div>
      </div>
    </#list>
  <div style="clear: both"></div>
    <#if cparam.showPagination>
        <#include "../include/pagination.ftl">
    </#if>
</div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/commerce-products-grid.png'/>"> Click to edit Product Grid
</div>
</#if>


