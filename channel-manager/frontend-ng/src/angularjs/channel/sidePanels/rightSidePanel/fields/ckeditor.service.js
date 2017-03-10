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

class CKEditorService {

  constructor($log, $q, $timeout, $window, ConfigService, DomService, PathService) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$timeout = $timeout;
    this.$window = $window;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.PathService = PathService;
  }

  loadCKEditor() {
    if (this.ckeditor) {
      return this.ckeditor;
    }

    this.ckeditor = this.DomService.addScript(this.$window, this._getCKEditorUrl())
      .then(() => {
        this._setCKEditorTimestamp();
        return this._ckeditorLoaded();
      });

    return this.ckeditor;
  }

  _getCKEditorUrl() {
    return this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), this.ConfigService.ckeditorUrl);
  }

  _setCKEditorTimestamp() {
    this.$window.CKEDITOR.timestamp = this.ConfigService.ckeditorTimestamp;
  }

  _ckeditorLoaded() {
    let pollTimeoutMillis = 2;
    const ready = this.$q.defer();

    const checkCKEditorLoaded = () => {
      if (typeof this.$window.CKEDITOR.on === 'function') {
        if (this.$window.CKEDITOR.status === 'loaded') {
          ready.resolve(this.$window.CKEDITOR);
        } else {
          this.$window.CKEDITOR.on('loaded', () => {
            ready.resolve(this.$window.CKEDITOR);
          });
        }
      } else {
        // try again using exponential backoff
        pollTimeoutMillis *= 2;
        this.$log.info(`Waiting ${pollTimeoutMillis} ms for CKEditor's event mechanism to load...`);
        this.$timeout(checkCKEditorLoaded, pollTimeoutMillis);
      }
    };

    checkCKEditorLoaded();

    return ready.promise;
  }

  getConfigByType(type) {
    const CKEDITOR = this.ckeditor || this.loadCKEditor();

    // TODO: Instead of returning the objects explicitly,
    // TODO: the configuratoin will be returned from the back-end.

    let config = null;

    if (type === 'rich-text') {
      // Rich text configuration
      config = {
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
        language: this.ConfigService.locale,
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
    } else if (type === 'formatted-text') {
      // Formatted text configuration
      config = {
        autoUpdateElement: false,
        entities: false,
        basicEntities: true,
        customConfig: '',
        language: this.ConfigService.locale,
        plugins: 'basicstyles,button,clipboard,contextmenu,divarea,enterkey,entities,floatingspace,floatpanel,htmlwriter,listblock,magicline,menu,menubutton,panel,panelbutton,removeformat,richcombo,stylescombo,tab,toolbar,undo',
        title: false,
        toolbar: [
          { name: 'styles', items: ['Styles'] },
          { name: 'basicstyles', items: ['Bold', 'Italic', 'Underline', '-', 'RemoveFormat'] },
          { name: 'clipboard', items: ['Undo', 'Redo'] },
        ],
      };
    }

    return config;
  }
}

export default CKEditorService;
