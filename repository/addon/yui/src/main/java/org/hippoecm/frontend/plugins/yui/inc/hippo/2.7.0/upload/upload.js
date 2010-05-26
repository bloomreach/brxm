/**
 * @description
 * <p>
 * Provides a singleton upload helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hippoajax, uploader, datatable, button 
 * @module upload
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.Upload) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.UploadImpl = function() {
            YAHOO.widget.Uploader.SWFURL = "http://yui.yahooapis.com/2.8.1/build/uploader/assets/uploader.swf";
            console.log('Flash url is set!');

            this.latest = null;

            this.entries = new YAHOO.hippo.HashMap();
        };

        YAHOO.hippo.UploadImpl.prototype = {
            register : function(id, config) {
                if(!this.entries.containsKey(id)) {
                    this.entries.put(id, {id: id, config: config});
                }
            },

            render : function() {
                this.entries.forEach(function(k, v) {
                    var id = v.id;
                    var config = v.config;
                    var el = Dom.get(id);
                    if(el != null) {
                        if (Lang.isUndefined(el.uploadWidget)) {
                            el.uploadWidget = new YAHOO.hippo.UploadWidget(id, config);
                        } else {
                            el.uploadWidget.update();
                        }
                        YAHOO.hippo.Upload.latest = el.uploadWidget;
                    } else {
                        YAHOO.log("Failed to render upload widget, element[" + id + "] not found", "error");
                    }
                });
            },

            upload : function() {
                if(this.latest != null) {
                    this.latest.upload();
                }
            }
        };

        YAHOO.hippo.UploadWidget = function(id, config) {
            this.id = id;
            this.config = config;

            var uiLayer = YAHOO.util.Dom.getRegion('selectLink');
            var overlay = YAHOO.util.Dom.get('uploaderOverlay');
            YAHOO.util.Dom.setStyle(overlay, 'width', uiLayer.right-uiLayer.left + "px");
            YAHOO.util.Dom.setStyle(overlay, 'height', uiLayer.bottom-uiLayer.top + "px");

            this.uploader = new YAHOO.widget.Uploader( "uploaderOverlay" );
            this.fileList = null;


            this.uploader.addListener('contentReady', this.handleContentReady);
            this.uploader.addListener('fileSelect', this.onFileSelect)
            this.uploader.addListener('uploadStart', this.onUploadStart);
            this.uploader.addListener('uploadProgress', this.onUploadProgress);
            this.uploader.addListener('uploadCancel', this.onUploadCancel);
            this.uploader.addListener('uploadComplete', this.onUploadComplete);
            this.uploader.addListener('uploadCompleteData', this.onUploadResponse);
            this.uploader.addListener('uploadError', this.onUploadError);
            this.uploader.addListener('rollOver', this.handleRollOver);
            this.uploader.addListener('rollOut', this.handleRollOut);
            this.uploader.addListener('click', this.handleClick);
            this.uploader.addListener('mouseDown', this.handleMouseDown);
            this.uploader.addListener('mouseUp', this.handleMouseUp);


            YAHOO.hippo.HippoAjax.registerDestroyFunction(Dom.get(this.id), this.destroy, this);
        };

        YAHOO.hippo.UploadWidget.prototype = {

            upload : function() {
                this.uploader.uploadAll("http://www.yswfblog.com/upload/upload_simple.php")
            },

            onFileSelect : function(event) {
                if('fileList' in event && event.fileList != null) {
	                  this.fileList = event.fileList;
    	              this._createDataTable(this.fileList);
                }
            },

            onUploadStart : function() {

            },

            onUploadProgress : function(event) {
                var rowNum = this.fileIdHash[event["id"]];
                var prog = Math.round(100*(event["bytesLoaded"]/event["bytesTotal"]));
                var progbar = "<div style='height:5px;width:100px;background-color:#CCC;'><div style='height:5px;background-color:#F00;width:" + prog + "px;'></div></div>";
                this.singleSelectDataTable.updateRow(rowNum, {name: this.dataArr[rowNum]["name"], size: this.dataArr[rowNum]["size"], progress: progbar});
            },

            onUploadCancel : function() {

            },

            onUploadComplete  : function(event) {
                var rowNum = this.fileIdHash[event["id"]];
                var prog = Math.round(100*(event["bytesLoaded"]/event["bytesTotal"]));
                var progbar = "<div style='height:5px;width:100px;background-color:#CCC;'><div style='height:5px;background-color:#F00;width:100px;'></div></div>";
                this.singleSelectDataTable.updateRow(rowNum, {name: this.dataArr[rowNum]["name"], size: this.dataArr[rowNum]["size"], progress: progbar});

            },

            onUploadResponse : function() {

            },

            onUploadError : function() {

            },

            handleContentReady  : function() {
                // Allows the uploader to send log messages to trace, as well as to YAHOO.log
                this.uploader.setAllowLogging(true);

                // Allows multiple file selection in "Browse" dialog.
                this.uploader.setAllowMultipleFiles(true);

                // New set of file filters.
                var ff = new Array({description:"Images", extensions:"*.jpg;*.png;*.gif"},
                                   {description:"Videos", extensions:"*.avi;*.mov;*.mpg"});

                // Apply new set of file filters to the uploader.
                this.uploader.setFileFilters(ff);
            },

            handleRollOver : function() {
                YAHOO.util.Dom.setStyle(YAHOO.util.Dom.get('selectLink'), 'color', "#FFFFFF");
                YAHOO.util.Dom.setStyle(YAHOO.util.Dom.get('selectLink'), 'background-color', "#000000");
            },

            handleRollOut : function() {
                YAHOO.util.Dom.setStyle(YAHOO.util.Dom.get('selectLink'), 'color', "#0000CC");
                YAHOO.util.Dom.setStyle(YAHOO.util.Dom.get('selectLink'), 'background-color', "#FFFFFF"); 
            },

            handleClick : function() {

            },

            handleMouseDown : function() {

            },

            handleMouseUp : function() {

            },

            _createDatatable : function(entries) {
                rowCounter = 0;
                this.fileIdHash = {};
                this.dataArr = [];
                for(var i in entries) {
                    var entry = entries[i];
                    entry["progress"] = "<div style='height:5px;width:100px;background-color:#CCC;'></div>";
                    this.dataArr.unshift(entry);
                }

                for (var j = 0; j < this.dataArr.length; j++) {
                    this.fileIdHash[this.dataArr[j].id] = j;
                }

                var myColumnDefs = [
                    {key:"name", label: "File Name", sortable:false},
                    {key:"size", label: "Size", sortable:false},
                    {key:"progress", label: "Upload progress", sortable:false}
                ];

                var myDataSource = new YAHOO.util.DataSource(this.dataArr);
                myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
                myDataSource.responseSchema = {
                  fields: ["id","name","created","modified","type", "size", "progress"]
                };

                this.singleSelectDataTable = new YAHOO.widget.DataTable("dataTableContainer",
                       myColumnDefs, myDataSource, {
                           caption:"Files To Upload",
                           selectionMode:"single"
                       });
            },

            destroy : function() {

                if(this.uploader != null) {
                    this.uploader.destroy();
                    console.log('Uploader destroyed!');
                    this.uploader = null;
                }

                this.config = null;
                this.id = null;
            }
        };
    })();

   YAHOO.hippo.Upload = new YAHOO.hippo.UploadImpl();

   YAHOO.register("Upload", YAHOO.hippo.Upload, {
       version: "2.8.1", build: "19"
   });
}
