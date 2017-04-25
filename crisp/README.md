# hippo-addon-crisp

Hippo Addon CRISP (Common Resource Interface and Service Provider).

## TODOs

- Document how to implement a web hook:

```java
    ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
    // clear all the cache for instance...
    resourceServiceBroker.getResourceDataCache("demoProductCatalogs").clear();
```

- Document how to use convenient property access by relative path through ```Resource#getValue(String relPath)```.
- Javadocs and documentation (how to configure json api rest service backend, caching, OAuth2 configuration, how to add custom ```ResourceResolver``` for non-JSON-API backends such as RDBMS, NoSQL Database, etc.)
- Web-hook example

## Build and install the module itself into local maven repository

```bash
    $ mvn clean install
```

## Running the demo locally

The demo project is located udner demo/ folder. So, move to the demo/ folder to run it locally.

```bash
    $ cd demo
    $ mvn clean verify
    $ mvn -P cargo.run
```

After startup, access the CMS at http://localhost:8080/cms and the site at http://localhost:8080/site.
Logs are located in target/tomcat8x/logs

## Generate Site Documentation

```bash
    $ mvn site
```

Open ```target/site/index.html``` after generation succeeds.

You can also generate a PDF document:

```bash
    $ mvn pdf:pdf
```

Open ```target/pdf/hippo-addon-crisp.pdf``` after generation succeeds.

## Demo Scenarios

- Visit http://localhost:8080/site/news.
- Click on a news article.
- Scroll down to the bottom, and you will see all the product list with generated links.

- Visit http://localhost:8080/cms/
- Select and edit an news document.
- Select 'Related Products' field and try to add some products through the pop up dialog (part of External Document Picker plugin).

Both product list results are from ```ResourceServiceBroker``` service component. See the code example section below for details.

## How to Use the ResourceBrokerService?

### In SITE application

If you want to use ```ResourceServiceBroker``` API in SITE application (e.g, HstComponent, Relevance collector module, etc.),
then you can use it like the following example (excerpt from [NewsContentComponent.java](demo/site/src/main/java/org/onehippo/cms7/crisp/demo/components/NewsContentComponent.java):

```java
    ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
    final Map<String, Object> pathVars = new HashMap<>();
    pathVars.put("fullTextSearchTerm", document.getTitle());
    ResourceContainer productCatalogs =
        resourceServiceBroker.findResources("demoProductCatalogs", "/products?q={fullTextSearchTerm}", pathVars);
    request.setAttribute("demoProductCatalogs", productCatalogs);
```

You can access the resource objects and generate links in freemarker templates like the following examples:

```
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
```

**Note**: <@crisp.link /> tag requires ```resourceSpace``` and ```resoruce``` attribute, and it may embed
<@crisp.variable /> tag(s) to pass variables to the underlying ```ResourceLinkResolver```.

### In CMS application

If you want to use ```ResourceServiceBroker``` API in CMS application (e.g, plugin, external document picker
service facade implementation, etc.), then you can use it like the following example
(excerpt from [CommerceProductDataServiceFacade.java](demo/cms/src/main/java/org/onehippo/cms7/crisp/demo/cms/plugin/CommerceProductDataServiceFacade.java):

```java
    private ResourceContainer findAllProductResources(final String queryString) {
        ResourceServiceBroker broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
        Map<String, Object> variables = new HashMap<>();
        variables.put("queryString", StringUtils.isNotBlank(queryString) ? queryString : "");
        return broker.findResources("productCatalogs", "/products?q={queryString}", variables);
    }
```
