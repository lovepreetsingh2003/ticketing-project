(function (window) {
    'use strict';

    var CSRF_TOKEN_URL = '/libs/granite/csrf/token.json';
    var csrfTokenPromise = null;

    function getCsrfToken() {
        if (!csrfTokenPromise) {
            csrfTokenPromise = fetch(CSRF_TOKEN_URL, { credentials: 'same-origin' })
                .then(function (res) {
                    if (!res.ok) {
                        throw new Error('Failed to fetch CSRF token');
                    }
                    return res.json();
                })
                .then(function (data) { return data.token; });
        }
        return csrfTokenPromise;
    }

    function postForm(url, body) {
        return getCsrfToken().then(function (token) {
            return fetch(url, {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'CSRF-Token': token
                },
                body: body
            });
        });
    }

    window.TicketingCsrf = {
        getCsrfToken: getCsrfToken,
        postForm: postForm
    };
}(window));
