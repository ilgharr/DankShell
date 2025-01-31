import { useEffect } from "react";
import { useCookies } from "react-cookie";
import { useLocation, useNavigate } from "react-router-dom"

const Callback = ({ userId }) => {
    const [cookies, setCookie] = useCookies(["userId"]);
    const location = useLocation();
    const navigate = useNavigate();


    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const code = params.get("code");
        const error = params.get("error");

        if(!code || error){
            console.warn("Error or missing code returned in callback:", error);
            navigate("/", { state: { error: "Authentication Failed" } }); // Redirect to home or error page
            return;
        }

        (async () => {
            try {
                const response = await fetch(`/api/callback?code=${code}`, {method: "GET"});
                if (response.ok) {
                    const userId = await response .text();
                    setCookie("userId", userId, {path: "/", maxAge: 432000});
//                    navigate("/home");
                } else {
                    console.error("Failed to retrieve user ID from server. Status:", response.status);
                    navigate("/", { state: { error: "Failed to retrieve user information." } });
                }
            } catch (e) {
                console.error("Error communicating with backend:", e);
                navigate("/", { state: { error: "Unexpected error occurred." } });
            }
        })();
    }, [location, setCookie, navigate]);

    return (
<div>
            <h2>Callback Endpoint</h2>

            {cookies.userId ? (
                // Display the userId cookie (if set)
                <p>Cookie has been set for User ID: {cookies.userId}</p>
            ) : (
                // Waiting for the userId to be fetched and set
                <p>Waiting for user ID...</p>
            )}
        </div>
    );
};

export default Callback;