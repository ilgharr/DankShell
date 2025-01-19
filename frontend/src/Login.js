import React from "react"

function Login() {
  const handleLogin = () => {
    window.location.href = "/login";
  };
    return <button onClick={handleLogin}>Login</button>;
}

export default Login