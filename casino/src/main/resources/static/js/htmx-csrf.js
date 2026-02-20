document.addEventListener('htmx:configRequest', function (event) {
    const csrfDiv = document.querySelector('#csrf-token');

    if (csrfDiv) {
        const token = csrfDiv.dataset.token;
        const headerName = csrfDiv.dataset.header;

        if (token && headerName) {
            event.detail.headers[headerName] = token;
        }
    }
});
