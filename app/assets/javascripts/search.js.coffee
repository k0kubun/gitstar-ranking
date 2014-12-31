jQuery ($) ->
  return if $('.searches_controller.show_action').length == 0

  $('.search_count_loader').each( ->
    url    = $(this).data('load-path')
    target = $(this).data('target')
    $.ajax(
      url,
      type: 'get'
      success: (data) ->
        $(target).removeClass('hidden')
        $(target).text(data)
    )
  )
