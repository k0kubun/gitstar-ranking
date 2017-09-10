import React from 'react';
import ReactDOM from 'react-dom';
import UpdateButton from '../../components/update_button';

document.addEventListener('DOMContentLoaded', function() {
  var mountNode = document.getElementById('update_button');
  if (mountNode != null) {
    ReactDOM.render(
      React.createElement(UpdateButton, null, null),
      mountNode
    );
  }
});
