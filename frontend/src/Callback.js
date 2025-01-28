import React, { useEffect, useState } from "react";
import { useCookies } from "react-cookie";

const Callback = () => {
  const [response, setResponse] = useState(null);


  useEffect(() => {
    fetch(window.location.href, {
      method: "GET",
    })
      .then((res) => res.json()) // convert response to text
      .then((data) => setResponse(data)) // store response in the state
      .catch((err) => console.error(err));
  }, []);


  return (
    <div>
      <h1>Callback</h1>
      <p>{response}</p>
    </div>
  );
}

export default Callback;

