import React, { PropTypes } from 'react';

export default class UpdateButton extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return React.createElement('a',
      {className: 'btn btn-info col-xs-12', href: '/update_myself', method: 'post'},
      'Update your stars'
    );
  }
}
