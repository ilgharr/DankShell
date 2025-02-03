import React from 'react';

const Landing = () => {

    const handleLogin = () => {
        window.location.href = '/login';
    }

    return (
        <div>
            <h1>HELLO WORLD!</h1>

            <button onClick={handleLogin}>Login</button>
        </div>
    );
};

export default Landing;