<#include "../include/imports.ftl">

<#if document??>
  <article class="well well-large">
  <@hst.manageContent hippobean=document />
  <header>
  <#if document.title??>
    <h2>${document.title}</h2>
  </#if>
  </header>
  <#if document.contentBlocks??>
    <#list document.contentBlocks as item>
      <#if item??>
        <#if item.type=='text'>
          <@hst.html hippohtml=item.text/>
        </#if>
        <#if item.type=='image'>
          <#if item.image?? && item.image.original??>
            <@hst.link var="img" hippobean=item.image.original/>
            <figure>
              <img src="${img}" title="${item.image.fileName}" alt="${item.image.fileName}"/>
              <#if item.image.description??><figcaption>${item.image.description}</figcaption></#if>
            </figure>
          </#if>
        </#if>
        <#if item.type=='video'>
          <iframe width="560" height="315" src="//www.youtube.com/embed/${item.video}" frameborder="0" allowfullscreen></iframe>
        </#if>
      </#if>
    </#list>
  </#if>
  </article>

</#if>