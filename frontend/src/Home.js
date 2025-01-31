import React from "react";
import { useLocation } from "react-router-dom";

const Home = () => {
    // Use the `useLocation` hook to extract query parameters
    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const isLoggedIn = queryParams.get("loggedIn") === "true";

    return (
        <div>
            <h1>{isLoggedIn ? "Logged In!" : "Not Logged In!"}</h1>
        </div>
    );
};

export default Home;