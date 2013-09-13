$(function() {

	$('#simple-colorpicker-1').ace_colorpicker({pull_right:true}).on('change', function(){
		var color_class = $(this).find('option:selected').data('class');
		var new_class = 'widget-header';
		if(color_class != 'default')  new_class += ' header-color-'+color_class;
		$(this).closest('.widget-header').attr('class', new_class);
	});


	// scrollables
	$('.slim-scroll').each(function () {
		var $this = $(this);
		$this.slimScroll({
			height: $this.data('height') || 100,
			railVisible:true
		});
	});

	  
	  
	/*
	$( '.row-fluid' ).sortable({
		connectWith: '.row-fluid'
	});
	$( ".widget-box" ).addClass( "ui-widget ui-widget-content ui-helper-clearfix ui-corner-all" )
	.find( ".widget-header" )
	.addClass( "ui-widget-header ui-corner-all" )
	.prepend( "<span class='ui-icon ui-icon-minusthick'></span>")
	.end()
	.find( ".widget-body" );
	$( ".portlet-header .ui-icon" ).click(function() {
	$( this ).toggleClass( "ui-icon-minusthick" ).toggleClass( "ui-icon-plusthick" );
	$( this ).parents( ".portlet:first" ).find( ".portlet-content" ).toggle();
	});
	$( '.row-fluid' ).disableSelection();
	*/

});