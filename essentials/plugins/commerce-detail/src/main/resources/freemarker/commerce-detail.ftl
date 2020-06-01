<#include "../include/imports.ftl">


<#-- @ftlvariable name="document" type="com.bloomreach.demo.beans.NewsDocument" -->
<#-- @ftlvariable name="item" type="com.onehippo.cms7.personalization.Product" -->
<#-- @ftlvariable name="link" type="com.onehippo.cms7.personalization.Sort" -->
<#-- @ftlvariable name="productsPageable" type="com.onehippo.cms7.personalization.ProductPageable" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
    <#list pageable.items as item>
        <h1>${item.title}</h1>
    </#list>
</#if>

<#if document??>

<div class="products-container">

      <div class="product product-flow">
          <img src="${document.image}" width="150" height="150"/>
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
  </#if>

</div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#if editMode>
<div>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/commerce-detail.png'/>">
    Click to edit product detail
</div>
</#if>


