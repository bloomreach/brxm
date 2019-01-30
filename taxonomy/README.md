# Taxonomy Plugin

This is the Taxonomy plugin for Hippo CMS. Documentation on how to use this
plugin in your Hippo project can be found at:

http://www.onehippo.org/library/concepts/plugins/taxonomy/about.html

## Build and Run the Demo Project

1. Create a demo project from the archetype as described at https://www.onehippo.org/trails/getting-started/hippo-essentials-getting-started.html
 
2. Add following dependency to *essentials/pom.xml*
```xml
<dependency>
  <groupId>org.onehippo.cms7</groupId>
  <artifactId>hippo-plugin-taxonomy-essentials-demo-feature</artifactId>
  <version>${hippo.plugin.taxonomy.version}</version>     
</dependency>
```

3. Build and run locally.
- From Essentials Library, select Taxonomy Demo. 
- Rebuild and restart, twice.
 
The project now contains:
 - A taxonomy tree
 - A taxonomy field in a news document type
 - A category filter in the CMS, hiding all categories except first level if the user is part of the author group
 - A taxonomy page at /taxonomy
 - A search page at /search?query=humor
  