jQuery ($) ->
  return if $('.users_controller.show_action').length == 0

  if $('#user_profile.updating').length > 0
    setTimeout(->
      location.reload()
    , 3000)
