<#include "../include/imports.ftl">
<#if document??>
  <article class="well well-large">
  <@hst.manageContent hippobean=document />
    <header>
      <h2>${document.title}</h2>
      <#if document.date ?? && document.date.time??>
        <p class="badge badge-info">
          <@fmt.formatDate value=document.date.time type="both" dateStyle="medium" timeStyle="short"/>
        </p>
      </#if>
      <#if document.summary??>
        <p>${document.summary}</p>
      </#if>
    </header>
    <@hst.html hippohtml=document.html/>
    <#if document.image?? && document.image.original??>
      <@hst.link var="img" hippobean=document.image.original/>
      <figure>
        <img src="${img}" title="${document.image.fileName}" alt="${document.image.fileName}" />
        <figcaption>${document.image.description}</figcaption>
      </figure>
    </#if>
    <#if document.texts??>
      <#list document.texts as item>
        <#if item.text??>
          <@hst.html hippohtml=item.text/>
        </#if>
      </#list>
    </#if>
  </article>
  <#if document.videos??>
    <#list document.videos as item>
      <#if item.video??>
        <iframe width="560" height="315" src="//www.youtube.com/embed/${item.video}" frameborder="0" allowfullscreen></iframe>
      </#if>
    </#list>
  </#if>
  <#if document.images??>
    <#list document.images as item>
      <#if item.image?? && item.image.original??>
        <@hst.link var="img" hippobean=item.image.original/>
        <img class="span5" src="${img}" title="${item.image.fileName}" alt="${item.image.fileName}"/>
      </#if>
    </#list>
  </#if>
</#if>
