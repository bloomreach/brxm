$(function(){
    function initToolbarBootstrapBindings() {
      var fonts = ['Arial', 'Courier', 'Comic Sans MS', 'Helvetica', 'Open Sans', 'Tahoma', 'Verdana'],
            fontTarget = $('[title=Font]').siblings('.dropdown-menu');
      $.each(fonts, function (idx, fontName) {
          fontTarget.append($('<li><a data-edit="fontName ' + fontName +'" style="font-family:\''+ fontName +'\'">'+fontName + '</a></li>'));
      });
      $('a[title]').tooltip({container:'body',animation:false});
    	$('.dropdown-menu input').click(function() {return false;})
		    .change(function () {$(this).parent('.dropdown-menu').siblings('.dropdown-toggle').dropdown('toggle');})
        .keydown('esc', function () {this.value='';$(this).change();});


      $('.wysiwyg-toolbar input[type=file]').prev().on('click', function () { 
        $(this).next().click();//the image icon
      });

	  $('#colorpicker').ace_colorpicker({pull_right:true,caret:false}).change(function(){
		$(this).nextAll('input').eq(0).val(this.value).change();
	  }).next().tooltip({title: $('#colorpicker').attr('title'), container:'body',animation:false}).next().hide();


      if ("onwebkitspeechchange"  in document.createElement("input")) {
        var editorOffset = $('#editor1').offset();
        $('#voiceBtn').css('position','absolute').offset({top: editorOffset.top, left: editorOffset.left+$('#editor1').innerWidth()-35});
      } else {
        $('#voiceBtn').hide();
      }
	};
	function showErrorAlert (reason, detail) {
		var msg='';
		if (reason==='unsupported-file-type') { msg = "Unsupported format " +detail; }
		else {
			console.log("error uploading file", reason, detail);
		}
		$('<div class="alert"> <button type="button" class="close" data-dismiss="alert">&times;</button>'+ 
		 '<strong>File upload error</strong> '+msg+' </div>').prependTo('#alerts');
	};
    initToolbarBootstrapBindings();  
	$('#editor1').wysiwyg( { fileUploadError: showErrorAlert , activeToolbarClass: 'active' , toolbarSelector : '#editor-toolbar-1'} );
	$('#editor2').wysiwyg( { fileUploadError: showErrorAlert , activeToolbarClass: 'active' , toolbarSelector : '#editor-toolbar-2' } );

	
	
	$('[data-toggle="buttons-radio"]').on('click', function(e){
		var target = $(e.target);
		var which = parseInt(target.html());
		var toolbar = $('#editor-toolbar-1').get(0);
		if(which == 1 || which == 2 || which == 3) {
			toolbar.className = toolbar.className.replace(/wysiwyg\-style(1|2)/g , '');
			if(which == 1) $(toolbar).addClass('wysiwyg-style1');
			else if(which == 2) $(toolbar).addClass('wysiwyg-style2');
		}
	});
});