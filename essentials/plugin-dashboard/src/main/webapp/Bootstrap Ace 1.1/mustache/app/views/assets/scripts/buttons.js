$(function() {
	$('#loading-btn').on('click', function () {
		var btn = $(this);
		btn.button('loading')
		setTimeout(function () {
			btn.button('reset')
		}, 2000)
	});

	$('#id-button-borders').attr('checked' , 'checked').change(function(){
			$('#default-buttons .btn').toggleClass('no-border');
	});
})