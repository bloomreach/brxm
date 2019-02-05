<#include "../include/imports.ftl">
<#assign fn=JspTaglibs ["http://java.sun.com/jsp/jstl/functions"] >

<@hst.defineObjects/>


<#--<c:set var="categoryInfo" value="${category.infos[hstRequest.locale]}" />-->

<div>
  <h2>Taxonomy Category: ${category.name} <strong>${locale}</strong></h2>
  <#if categoryInfo??>
    <ul>
    <li>Name: ${categoryInfo.name}</li>
    <li>Description:
    <pre><#if categoryInfo.description??>${categoryInfo.description}</#if></pre></li>
  <li>Synonyms:</li>
    <#list categoryInfo.synonyms as synonym>
      <li>${synonym}</li>
    </#list>
    </ul>
  </#if>
  <#if subCategories??>
    <#list subCategories as category>
      <@hst.link var="link" path="taxonomy"/>
      <li><a href="${link}?category=${category.link}">${category.link}</a></li>
    </#list>
  </#if>
    <hr/>

    <h2>Documents in this taxonomy category:</h2>

  <#if documents??>
    <ul>
    <#list documents as bean>
      <li>
      <@hst.link var="link" hippobean=bean />
    <a href="${link}">${bean.title}</a>
      </li>
    </#list>
    </ul>
  </#if>

</div>

