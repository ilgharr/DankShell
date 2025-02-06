import React from 'react';
import { Navbar, Container, Button } from 'react-bootstrap';

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
               <Container className="landing-page-info">
                    <p>"Simple and Secure Cloud Storage for Everything You Need!"</p>
                    <p>"Access your files anywhere, anytime. Safe, fast, and hassle-free."</p>
                </Container>
            <Footer/>
        </div>
    );
};

export default Landing;