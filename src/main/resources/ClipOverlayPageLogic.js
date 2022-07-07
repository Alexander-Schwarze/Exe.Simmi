window.onload = () => {
    const webSocket = new WebSocket(`ws://localhost:${serverPort}/socket`);
    const videoPlayer = document.querySelector('#video-player');
    const warning = document.querySelector('#warning');


    videoPlayer.onended = () => {
        webSocket.send('next video lol');
    };

    webSocket.onopen = () => {
        warning.style.visibility = 'hidden';
        webSocket.send('next video lol');
    };

    webSocket.onmessage = message => {
        videoPlayer.src = `/video/${message.data}`;
        videoPlayer.play();
    };

    webSocket.onclose = webSocket.onerror = () => {
        warning.style.visibility = 'visible';
    };
};