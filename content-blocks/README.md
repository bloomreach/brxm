# Content Blocks Plugin

This is the Content Blocks plugin for Hippo CMS. Documentation on how to use this
plugin in your Hippo project can be found at:

http://www.onehippo.org/library/concepts/plugins/content-blocks/about.html

## Build and Run the Demo Project

1. Create a demo project from the archetype as described at https://www.onehippo.org/trails/getting-started/hippo-essentials-getting-started.html
 
2. Add following dependency to *essentials/pom.xml*
```xml
<dependency>
  <groupId>org.onehippo.cms7</groupId>
  <artifactId>hippo-plugin-content-blocks-essentials-demo-feature</artifactId>
  <version>${hippo.plugin.content-blocks.version}</version>     
</dependency>
```

3. Build and run locally.
- From Essentials Library, select Content Blocks Demo. 
- Rebuild and restart.
 
The project now contains some document type plus document instances containing content blocks and a site that renders those