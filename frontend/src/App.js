import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Home from './Home'
import Landing from './Landing'
import Callback from './Callback'

const App = () => {
    return (
        <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/home" element={<Home />} />
            <Route path="/callback" element={<Callback />} />
        </Routes>
    );
};

export default App;