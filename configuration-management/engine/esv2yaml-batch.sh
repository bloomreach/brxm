#!/bin/bash
./esv2yaml.sh cms-community hippo-repository workflow
./esv2yaml.sh cms-community hippo-repository testutils
./esv2yaml.sh cms-community hippo-repository scripts
./esv2yaml.sh cms-community hippo-repository upgrade
./esv2yaml.sh cms-community hippo-repository config
./esv2yaml.sh cms-community hippo-repository testcontent
./esv2yaml.sh cms-community hippo-repository jaxrs
./esv2yaml.sh cms-community hippo-repository modules

./esv2yaml.sh cms-community hippo-cms scripts
./esv2yaml.sh cms-community hippo-cms config
./esv2yaml.sh cms-community hippo-cms automatic-export/repository
./esv2yaml.sh cms-community hippo-cms richtext/repository
./esv2yaml.sh cms-community hippo-cms test
./esv2yaml.sh cms-community hippo-cms workflow/repository
./esv2yaml.sh cms-community hippo-cms brokenlinks/repository
./esv2yaml.sh cms-community hippo-cms reporting/repository
./esv2yaml.sh cms-community hippo-cms types
./esv2yaml.sh cms-community hippo-cms gallery/repository
./esv2yaml.sh cms-community hippo-cms editor/repository
./esv2yaml.sh cms-community hippo-cms console/repository
./esv2yaml.sh cms-community hippo-cms translation/repository

./esv2yaml.sh cms-community hippo-site-toolkit client-modules/google-analytics/repository 
./esv2yaml.sh cms-community hippo-site-toolkit toolkit-resources/addon/unittestcontents
./esv2yaml.sh cms-community hippo-site-toolkit toolkit-resources/addon/repository
./esv2yaml.sh cms-community hippo-site-toolkit toolkit-resources/addon/toolkit-cnd/cnd
./esv2yaml.sh cms-community hippo-site-toolkit components/resourcebundle-cnd

