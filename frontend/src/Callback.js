import { useEffect, useState } from "react";
import { useCookies } from "react-cookie";

const Callback = () => {
    const [tokenData, setTokenData] = useState(null);
    const [cookies, setCookie, removeCookie] = useCookies(["user"]);

    useEffect(() => {
        const queryParams = new URLSearchParams(window.location.search);
        const code = queryParams.get("code");

        fetch(`/api/callback?code=${code}`)
            .then((response) => response.json())
            .then((data) => {
                setTokenData(data); // Store token data
            });
        setCookie("token", tokenData, {path: "/"})
    }, []);


    return (
        <div>
            <h1>Token Success</h1>
            {tokenData && <pre>{JSON.stringify(tokenData)}</pre>}
        </div>
    );
};

export default Callback;