

const express = require('express');
const { Issuer } = require('openid-client');
const path = require('path');
const app = express();

let client;

Issuer.discover('https://cognito-idp.ca-central-1.amazonaws.com/ca-central-1_GLNVaRj0r')
    .then(issuer => {
        client = new issuer.Client({
            client_id: '4lnf97fd4bs0emr40mfbriso4c',
            redirect_uris: ['http://localhost:8443/callback'],
            response_types: ['code'],
        });
        console.log('OpenID Client initialized successfully.');
    })
    .catch(err => console.error('Error initializing OpenID client:', err));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '/views/home.html')); // Serve index.html
});

app.get('/login', (req, res) => {
    if (!client) return res.status(500).send('Client not ready');
    res.redirect(client.authorizationUrl({ scope: 'openid email' }));
});

app.listen(8444, () => console.log('Server running on http://localhost:8444'));