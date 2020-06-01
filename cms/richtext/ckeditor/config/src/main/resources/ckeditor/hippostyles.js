/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

(function() {
    "use strict";

    var ELEMENTS = ['p','h1','h2','h3','h4','h5','h6','pre','address'],
        TRANSLATIONS = {
            de: {
                address: 'Adresse',
                h1: 'Überschrift 1',
                h2: 'Überschrift 2',
                h3: 'Überschrift 3',
                h4: 'Überschrift 4',
                h5: 'Überschrift 5',
                h6: 'Überschrift 6',
                p: 'Normal',
                pre: 'Formatiert'
            },
            en: {
                address: 'Address',
                h1: 'Heading 1',
                h2: 'Heading 2',
                h3: 'Heading 3',
                h4: 'Heading 4',
                h5: 'Heading 5',
                h6: 'Heading 6',
                p: 'Normal',
                pre: 'Preformatted Text'
            },
            fr: {
                address: 'Adresse',
                h1: 'En-tête 1',
                h2: 'En-tête 2',
                h3: 'En-tête 3',
                h4: 'En-tête 4',
                h5: 'En-tête 5',
                h6: 'En-tête 6',
                p: 'Normal',
                pre: 'Formaté'
            },
            nl: {
                address: 'Adres',
                h1: 'Kop 1',
                h2: 'Kop 2',
                h3: 'Kop 3',
                h4: 'Kop 4',
                h5: 'Kop 5',
                h6: 'Kop 6',
                p: 'Normaal',
                pre: 'Voorgedefinieerd'
            },
            es: {
                address: 'Dirección',
                h1: 'Encabezado 1',
                h2: 'Encabezado 2',
                h3: 'Encabezado 3',
                h4: 'Encabezado 4',
                h5: 'Encabezado 5',
                h6: 'Encabezado 6',
                p: 'Normal',
                pre: 'Texto preformateado'
            },
            zh: {
                address: '地址',
                h1: '标题一',
                h2: '标题二',
                h3: '标题三',
                h4: '标题四',
                h5: '标题五',
                h6: '标题六',
                p: '平常',
                pre: '预定模式'
            }
        },
        stylesSet, language, element, i, len;

    // add a separate styles resource per language
    for (language in TRANSLATIONS) {
        stylesSet = [];
        for (i = 0, len = ELEMENTS.length; i < len; i++) {
            element = ELEMENTS[i];
            stylesSet.push({
                element: element,
                name: TRANSLATIONS[language][element] || element
            });
        }
        CKEDITOR.stylesSet.add('hippo_' + language, stylesSet);
    }

}());
