<#include "../include/imports.ftl">

<h3>Products</h3>

<#if products?has_content??>
  <ul>
    <#list products as product>
      <li>
        [${product.sku!}] ${product.extendedData.title!}
        (${product.extendedData.description!})
      </li>
    </#list>
  </ul>
</#if>

<div>
  <@hst.include ref="container"/>
</div>
