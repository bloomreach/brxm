<#--
  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>

<#if title??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.document.title">
  <title>${title?xml}</title>
  </@hst.headContribution>
</#if>

<#if metaKeywords??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.document.keywords">
  <meta name="keywords" content="${metaKeywords?xml}"/>
  </@hst.headContribution>
</#if>

<#if metaDescription??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.document.description">
  <meta name="description" content="${metaDescription?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreSchemaLink??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.schema.dc">
  <link rel="schema.DC" href="http://purl.org/dc/elements/1.1/"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsLink??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.dc.terms">
  <link rel="schema.DCTERMS" href="http://purl.org/dc/terms/"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreCopyrightLink??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.dc.copyright">
  <link rel="DC.rights copyright" href="${dublinCoreCopyrightLink?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreLanguage??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.dc.language">
  <meta scheme="DCTERMS.RFC3066" name="DC.language" content="${dublinCoreLanguage?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsCreated??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.dc.terms.created">
  <meta name="DCTERMS.created" content="${dublinCoreTermsCreated?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsModified??>
  <@hst.headContribution category="htmlHead" keyHint="hst.seo.dc.terms.modified">
  <meta name="DCTERMS.modified" content="${dublinCoreTermsModified?xml}"/>
  </@hst.headContribution>
</#if>