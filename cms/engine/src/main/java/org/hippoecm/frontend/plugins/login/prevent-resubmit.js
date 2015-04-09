var Hippo = window.Hippo || {};

Hippo.submitting = false;
Hippo.onLoginSubmit = function() {
  if(Hippo.submitting) {
    console.log('duplicate login form submission prevented');
    return false;
  } else {
    $('${submitButtonId}').prop('disabled', true);
    Hippo.submitting = true;
    return true;
  }
}
