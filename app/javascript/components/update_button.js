import React from 'react';
import PropTypes from 'prop-types';

export default class UpdateButton extends React.Component {
  static propTypes = {
    label: PropTypes.string.isRequired,
    login: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired
  };

  constructor(props) {
    super(props);
    this.state = { updateStatus: 'OUTDATED' };

    this.hookUpdateStatus();
  }

  hookUpdateStatus() {
    setTimeout(function() {
      this.setState({ updateStatus: 'UPDATED' });
    }.bind(this), 3000);
  }

  render() {
    switch (this.state.updateStatus) {
      case 'UPDATING':
        return React.createElement('span',
          { className: 'btn btn-default disabled col-xs-12' },
          'Updating stars ...'
        );
      case 'UPDATED':
        return React.createElement('span',
          { className: 'btn btn-default disabled col-xs-12' },
          'Up to date'
        );
      default:
        return React.createElement('a',
          { className: 'btn btn-info col-xs-12', href: this.props.path, method: 'post' },
          this.props.label
        );
    }
  }
}
