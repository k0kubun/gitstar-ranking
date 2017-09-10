import React from 'react';
import ReactDOM from 'react-dom';
import UpdateButton from '../../components/update_button';

document.addEventListener('DOMContentLoaded', function() {
  var mountNode = document.getElementById('update_button');
  if (mountNode != null) {
    var label = mountNode.getAttribute('data-label');
    var login = mountNode.getAttribute('data-login');
    var path = mountNode.getAttribute('data-path');

    ReactDOM.render(
      React.createElement(UpdateButton, { label: label, login: login, path: path }, null),
      mountNode
    );
  }
});
