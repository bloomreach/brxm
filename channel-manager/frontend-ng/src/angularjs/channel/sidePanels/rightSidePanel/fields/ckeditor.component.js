/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import template from './ckeditor.html';

class ckeditorController {
  constructor($scope, $element, ConfigService, CKEditorService) {
    'ngInject';

    this.ConfigService = ConfigService;
    this.CKEditorService = CKEditorService;
    this.element = $element;
    this.scope = $scope;
  }

  $onInit() {
    this.testValue = { name: 'Ariel' };

    const ConfigService = this.ConfigService;
    const CKEditorService = this.CKEditorService;
    const element = this.element;

    CKEditorService.loadCKEditor().then((CKEDITOR) => {
      const editorConfig = {
        autoUpdateElement: false,
        entities: false,
        basicEntities: true,
        customConfig: '',
        dialog_buttonsOrder: 'ltr',
        dialog_noConfirmCancel: true,
        extraAllowedContent: 'embed[allowscriptaccess,height,src,type,width]; img[border,hspace,vspace]; object[align,data,height,id,title,type,width]; p[align]; param[name,value]; table[width]; td[valign,width]; th[valign,width];',
        keystrokes: [
          [CKEDITOR.CTRL + 77, 'maximize'],
          [CKEDITOR.ALT + 66, 'showblocks'],
        ],
        language: ConfigService.locale,
        linkShowAdvancedTab: false,
        plugins: 'a11yhelp,basicstyles,button,clipboard,codemirror,contextmenu,dialog,dialogadvtab,dialogui,divarea,elementspath,enterkey,entities,floatingspace,floatpanel,htmlwriter,indent,indentblock,indentlist,justify,link,list,listblock,liststyle,magicline,maximize,menu,menubutton,panel,panelbutton,pastefromword,pastetext,popup,removeformat,resize,richcombo,showblocks,showborders,specialchar,stylescombo,tab,table,tableresize,tabletools,textselection,toolbar,undo,youtube',
        removeFormatAttributes: 'style,lang,width,height,align,hspace,valign',
        title: false,
        toolbarGroups: [
          { name: 'styles' },
          { name: 'basicstyles' },
          { name: 'undo' },
          { name: 'listindentalign', groups: ['list', 'indent', 'align'] },
          { name: 'links' },
          { name: 'insert' },
          { name: 'tools' },
          { name: 'mode' },
        ],
      };

      // const textAreaElement = this.element

      const ckEditorElement = element[0];

      const textAreaElement = angular.element(ckEditorElement).find('textarea')[0];

      this.editor = CKEDITOR.replace(textAreaElement, editorConfig);

      this.editor.setData(this.model.$viewValue);

      this.editor.on('change', () => {
        this.scope.$evalAsync(() => {
          const html = this.editor.getData();
          this.model.$setViewValue(html);
        });
      });

      this.editor.on('focus', () => {
        this.onFocus();
        this.scope.$apply();
      });

      this.editor.on('blur', () => {
        this.onBlur();
        this.scope.$apply();
      });
    });

    this.scope.$on('$destroy', () => {
      this.editor.destroy();
    });
  }
}

const ckeditorComponent = {
  require: {
    model: 'ngModel',
  },
  template,
  controller: ckeditorController,
  bindings: {
    name: '@',
    ariaLabel: '@',
    isRequired: '@',
    onFocus: '&',
    onBlur: '&',
  },
};

export default ckeditorComponent;
