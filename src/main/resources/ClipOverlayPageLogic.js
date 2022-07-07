window.onload = () => {
    let webSocket = new WebSocket(`ws://localhost:${serverPort}/socket`);
    let reconnectInterval = null;

    const videoPlayer = document.querySelector('#video-player');
    const warning = document.querySelector('#warning');


    videoPlayer.onended = () => {
        webSocket.send('next video lol');
    };

    webSocket.onopen = () => {
        warning.style.visibility = 'hidden';
        webSocket.send('next video lol');

        reconnectInterval = null;
    };

    webSocket.onmessage = message => {
        videoPlayer.src = `/video/${message.data}`;
    };

    webSocket.onclose = webSocket.onerror = () => {
        warning.style.visibility = 'visible';

        reconnectInterval = window.setInterval(() => {
            webSocket = new WebSocket(`ws://localhost:${serverPort}/socket`);
        }, 5000);
    };
};