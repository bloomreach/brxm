/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

function ckeditor($window, ConfigService) {
  'ngInject';

  return {
    restrict: 'A',
    require: 'ngModel',
    link: (scope, element, attrs, ngModel) => {
      // TODO: get editor config from the REST response of the field instead of hard-coding the default 'formatted text' config
      const editorConfig = {
        autoUpdateElement: false,
        entities: false,
        basicEntities: true,
        customConfig: '',
        language: ConfigService.locale,
        plugins: 'basicstyles,button,clipboard,contextmenu,divarea,enterkey,entities,floatingspace,floatpanel,htmlwriter,listblock,magicline,menu,menubutton,panel,panelbutton,removeformat,richcombo,stylescombo,tab,toolbar,undo',
        title: false,
        toolbar: [
          { name: 'styles', items: ['Styles'] },
          { name: 'basicstyles', items: ['Bold', 'Italic', 'Underline', '-', 'RemoveFormat'] },
          { name: 'clipboard', items: ['Undo', 'Redo'] },
        ],
      };

      const editor = $window.CKEDITOR.replace(element[0], editorConfig);

      ngModel.$render = () => {
        editor.setData(ngModel.$viewValue);
      };

      editor.on('change', () => {
        scope.$evalAsync(() => {
          const html = editor.getData();
          ngModel.$setViewValue(html);
        });
      });
    },
  };
}

export default ckeditor;

