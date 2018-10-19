# BloomReach Essentials
BloomReach Essentials is for developers who want to setup a new [BloomReach CMS](https://www.onehippo.org) project. It enables
them to kickstart their project in a matter of minutes, to benefit from our best practices and to easily add Enterprise
or community plugins.

```
Please use the BloomReach Essentials feedback form to inform us if you encounter any bugs/glitches or if you have any
suggestions for improvements.
```

# Getting Started

## Code checkout

To get started with the BloomReach Essentials, checkout the code. You have two options to check out
the project. The example commands below use the potentially unstable trunk snapshot. Consider
using a tag instead.

### Build the essentials components:
```shell
cd hippo-essentials
mvn clean install
```

### Validate license headers:
```shell
mvn clean && mvn validate -Ppedantic
```

### Generate a new BloomReach project from the archetype:
See the [Getting Started](https://www.onehippo.org/trails/getting-started/hippo-essentials-getting-started.html) page.

##Automatic Export

Essentials depends on the automatic export feature being enabled, which is the archetype-generated BloomReach
project's default setting. You can change the setting temporarily in the upper right corner in the CMS,
or permanently in your project's file
`./repository-data/config/src/main/resources/configuration/modules/autoexport-module.xml`

##Copyright and license

Copyright 2013-2018 BloomReach B.V.
Distributed under the [Apache 2.0 license](https://code.onehippo.org/cms-community/hippo-essentials/blob/master/LICENSE).

