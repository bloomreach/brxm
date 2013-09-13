$(function() {

	$('#accordion2').on('hide', function (e) {
		$(e.target).prev().children(0).addClass('collapsed');
	})
	$('#accordion2').on('hidden', function (e) {
		$(e.target).prev().children(0).addClass('collapsed');
	})
	$('#accordion2').on('show', function (e) {
		$(e.target).prev().children(0).removeClass('collapsed');
	})
	$('#accordion2').on('shown', function (e) {
		$(e.target).prev().children(0).removeClass('collapsed');
	})


	var oldie = $.browser.msie && $.browser.version < 9;
	$('.easy-pie-chart.percentage').each(function(){
		$(this).easyPieChart({
			barColor: $(this).data('color'),
			trackColor: '#EEEEEE',
			scaleColor: false,
			lineCap: 'butt',
			lineWidth: 8,
			animate: oldie ? false : 1000,
			size:75
		}).css('color', $(this).data('color'));
	});

	$('[data-rel=tooltip]').tooltip();
	$('[data-rel=popover]').popover({html:true});


	$('#gritter-regular').click(function(){
		$.gritter.add({
			title: 'This is a regular notice!',
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="blue">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: $assets+'/avatars/avatar1.png',
			sticky: false,
			time: '',
			class_name: (!$('#gritter-light').get(0).checked ? 'gritter-light' : '')
		});

		return false;
	});

	$('#gritter-sticky').click(function(){
		var unique_id = $.gritter.add({
			title: 'This is a sticky notice!',
			text: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="red">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: $assets+'/avatars/avatar.png',
			sticky: true,
			time: '',
			class_name: 'gritter-info' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});


	$('#gritter-without-image').click(function(){
		$.gritter.add({
			// (string | mandatory) the heading of the notification
			title: 'This is a notice without an image!',
			// (string | mandatory) the text inside the notification
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="orange">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			class_name: 'gritter-success' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});


	$('#gritter-max3').click(function(){
		$.gritter.add({
			title: 'This is a notice with a max of 3 on screen at one time!',
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="green">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: $assets+'/avatars/avatar3.png',
			sticky: false,
			before_open: function(){
				if($('.gritter-item-wrapper').length >= 3)
				{
					return false;
				}
			},
			class_name: 'gritter-warning' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});


	$('#gritter-error').click(function(){
		$.gritter.add({
			title: 'This is a warning notification',
			text: 'Just add a "gritter-light" class_name to your $.gritter.add or globally to $.gritter.options.class_name',
			class_name: 'gritter-error' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});
		

	$("#gritter-remove").click(function(){
		$.gritter.removeAll();
		return false;
	});
		

	///////


	$("#bootbox-regular").on('click', function() {
		bootbox.prompt("What is your name?", function(result) {
			if (result === null) {
				//Example.show("Prompt dismissed");
			} else {
				//Example.show("Hi <b>"+result+"</b>");
			}
		});
	});
		
	$("#bootbox-confirm").on('click', function() {
		bootbox.confirm("Are you sure?", function(result) {
			if(result) {
				bootbox.alert("You are sure!");
			}
		});
	});
		
	$("#bootbox-options").on('click', function() {
		bootbox.dialog("I am a custom dialog with smaller buttons", [{
			"label" : "Success!",
			"class" : "btn-small btn-success",
			"callback": function() {
				//Example.show("great success");
			}
			}, {
			"label" : "Danger!",
			"class" : "btn-small btn-danger",
			"callback": function() {
				//Example.show("uh oh, look out!");
			}
			}, {
			"label" : "Click ME!",
			"class" : "btn-small btn-primary",
			"callback": function() {
				//Example.show("Primary button");
			}
			}, {
			"label" : "Just a button...",
			"class" : "btn-small"
			}]
		);
	});



	$('#spinner-opts small').css({display:'inline-block', width:'60px'})

	var slide_styles = ['', 'green','red','purple','orange', 'dark'];
	var ii = 0;
	$("#spinner-opts input[type=text]").each(function() {
		var $this = $(this);
		$this.hide().after('<span />');
		$this.next().addClass('ui-slider-small').
		addClass("inline ui-slider-"+slide_styles[ii++ % slide_styles.length]).
		css({'width':'125px'}).slider({
			value:parseInt($this.val()),
			range: "min",
			animate:true,
			min: parseInt($this.data('min')),
			max: parseInt($this.data('max')),
			step: parseFloat($this.data('step')),
			slide: function( event, ui ) {
				$this.attr('value', ui.value);
				spinner_update();
			}
		});
	});





	$.fn.spin = function(opts) {
		this.each(function() {
		  var $this = $(this),
			  data = $this.data();

		  if (data.spinner) {
			data.spinner.stop();
			delete data.spinner;
		  }
		  if (opts !== false) {
			data.spinner = new Spinner($.extend({color: $this.css('color')}, opts)).spin(this);
		  }
		});
		return this;
	};

	function spinner_update() {
		var opts = {};
		$('#spinner-opts input[type=text]').each(function() {
			opts[this.name] = parseFloat(this.value);
		});
		$('#spinner-preview').spin(opts);
	}



	$('#id-pills-stacked').removeAttr('checked').change(function(){
		$('.nav-pills').toggleClass('nav-stacked');
	});


});