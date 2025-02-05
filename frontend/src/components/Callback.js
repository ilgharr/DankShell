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

    const handleSendingId = async () => {
        const userId = cookies.userId;
        if(!userId){
            console.error("No userId available to send.");
            return;
        }
                try{
                    const response = await fetch("/getid", {
                            method: "POST",
                            headers:{"Content-Type": "application/json"},
                        body: JSON.stringify({ userId }),
                    });

                    if(response.ok){
                        console.log("User ID successfully sent to backend.");
                    } else {
                        console.error("Failed to send User ID. Status:", response.status);
                    }
                } catch (error) {
                    console.error("Error occurred when sending User ID to backend:", error);
                }
   }

    return (
        <div>
            <h2>Callback Endpoint</h2>

            {cookies.userId ? (
                <p>Cookie has been set for User ID: {cookies.userId}</p>
            ) : (
                <p>Waiting for user ID...</p>
            )}
            <button onClick={handleSendingId}>Send ID</button>
        </div>
    );
};

export default Callback;