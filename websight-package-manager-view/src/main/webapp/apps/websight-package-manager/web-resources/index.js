import React from 'react';
import ReactDOM from 'react-dom';

import 'websight-admin/GlobalStyle';

import PackageManager from './PackageManager.js';

class App extends React.Component {
    render() {
        return (
            <PackageManager/>
        );
    }
}

ReactDOM.render(<App/>, document.getElementById('app-root'));