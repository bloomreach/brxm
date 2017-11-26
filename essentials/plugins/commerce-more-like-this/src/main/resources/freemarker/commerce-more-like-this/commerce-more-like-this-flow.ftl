<#include "../../include/imports.ftl">

<#-- @ftlvariable name="item" type="com.onehippo.cms7.personalization.Product" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
<div class="products-container">
    <#list pageable.items as item>

      <div class="product product-flow">
        <img src="${item.image}" width="150" height="150"/>
        <div class="product-text">
          <div class="title">${item.title}</div>
          <div class="description">${item.description}</div>
        </div>
        <a class="btn btn-primary" href="${item.url}">$ ${item.price}</a>
        <div class="rating">
          <input name="r" type="radio" value="5"/>
          <span>☆</span><input name="r" type="radio" value="4" checked="checked"/>
          <span>☆</span><input name="r" type="radio" value="3"/>
          <span>☆</span><input name="r" type="radio" value="2"/>
          <span>☆</span><input name="r" type="radio" value="1"/>
          <span>☆</span>
        </div>
      </div>
    </#list>

</div>
    <#if cparam.showPagination>

        <#include "../../include/pagination.ftl">
    </#if>
    <@hst.headContribution category="htmlHead">
    <link rel="stylesheet" href="<@hst.webfile  path="/css/product-grid.css"/>" type="text/css"/>
    </@hst.headContribution>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/commerce-more-like-this.png'/>"> Click to edit
    More Like This
</div>
</#if>


