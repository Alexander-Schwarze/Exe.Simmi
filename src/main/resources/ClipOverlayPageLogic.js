window.onload = () => {
    // TODO: Get port from Config file
    const webSocket = new WebSocket('ws://localhost:12345/socket');
    const overlay = document.querySelector('#overlay');
    const warning = document.querySelector('#warning');

    webSocket.onopen = () => {
        warning.style.visibility = 'hidden';
    };

    webSocket.onmessage = event => {
    };

    webSocket.onclose = webSocket.onerror = () => {
        warning.style.visibility = 'visible';
    };
};