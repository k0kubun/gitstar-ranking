(function() {
  jQuery(function($) {
    if ($('#user_profile.updating').length > 0) {
      return setTimeout(function() {
        return location.reload();
      }, 3000);
    }
  });
}).call(this);
