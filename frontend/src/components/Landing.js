import React from 'react';
import LandingNavbar from './LandingNavbar'
import Footer from './Footer'
import "./CssHelper.js"
const Landing = () => {

    const handleLogin = () => {
        window.location.href = '/login';
    }

    return (
        <div>
            <LandingNavbar/>
            <div id="stars"></div>
            <script src="./CssHelper.js"></script>
            <Footer/>
        </div>
    );
};

export default Landing;