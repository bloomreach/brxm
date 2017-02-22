/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = {
    npmDir:      'node_modules',
    extjs: {
        src:    'src/main/resources/org/hippoecm/frontend/extjs/hippotheme/ExtHippoTheme.scss',
        target: 'target/classes/org/hippoecm/frontend/extjs/hippotheme/ExtHippoTheme.css'
    },
    file:       'hippo-cms-theme',
    images:     'target/classes/org/hippoecm/frontend/skin/images',
    skin:       'target/classes/skin/hippo-cms',
    src:        'src/main/styling',
    fileupload: {
        src:    'src/main/resources/org/hippoecm/frontend/plugins/jquery/upload',
        target: 'target/classes/org/hippoecm/frontend/plugins/jquery/upload'
    },
    target:     'target',
    tmp:        'target/.tmp',
    svgsprite:  'src/main/styling/svgsprite/**/*.svg'
};
