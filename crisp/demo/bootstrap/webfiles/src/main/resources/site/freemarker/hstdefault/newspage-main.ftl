<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="org.onehippo.cms7.crisp.demo.beans.NewsDocument" -->
<#if document??>
  <@hst.link var="link" hippobean=document/>
  <article class="has-edit-button">
    <@hst.cmseditlink hippobean=document/>
    <h3><a href="${link}">${document.title?html}</a></h3>
    <#if document.date??>
      <p><@fmt.formatDate value=document.date.time type="both" dateStyle="medium" timeStyle="short"/></p>
    </#if>
    <#if document.endDate??>
      <p><@fmt.formatDate value=document.endDate.time type="both" dateStyle="medium" timeStyle="short"/></p>
    </#if>
    <#if document.author??>
      <p>${document.author?html}</p>
    </#if>
    <#if document.source??>
      <p>${document.source?html}</p>
    </#if>
    <#if document.location??>
      <p>${document.location?html}</p>
    </#if>
    <#if document.introduction??>
      <p>${document.introduction?html}</p>
    </#if>
    <#if document.image?? && document.image.original??>
      <@hst.link var="img" hippobean=document.image.original/>
      <figure>
        <img src="${img}" title="${document.image.fileName?html}" alt="${document.image.fileName?html}"/>
        <#if document.image.description??>
          <figcaption>${document.image.description?html}</figcaption>
        </#if>
      </figure>
    </#if>
    <@hst.html hippohtml=document.content/>
  </article>

  <#if productCatalogs?? && productCatalogs.anyChildContained>
    <article class="has-edit-button">
      <h3>Related Products</h3>
      <ul>
        <#list productCatalogs.childIterator as product>
          <#assign extendedData=product.valueMap['extendedData'] />
          <li>
            <@crisp.link var="productLink" resourceSpace='demoProductCatalogs' resource=product>
              <@crisp.variable name="preview" value="${hstRequestContext.preview?then('true', 'false')}" />
              <@crisp.variable name="name" value="${product.valueMap['name']}" />
            </@crisp.link>
            <a href="${productLink}">
              ${extendedData.valueMap['title']!} (${product.valueMap['SKU']!})
            </a>
          </li>
        </#list>
      </ul>
    </article>
  </#if>

</#if>