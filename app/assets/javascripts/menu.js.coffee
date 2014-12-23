jQuery ($) ->
  # workaround for not working side menu
  menuSelector = '#side-nav .menu_item'
  $(document).delegate(menuSelector, 'click', (event) ->
    $(menuSelector).removeClass('active')
    $(this).closest('.menu_item').addClass('active')
  )
