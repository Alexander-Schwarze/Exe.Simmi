window.onload = () => {
    const webSocket = new WebSocket(`ws://localhost:${serverPort}/socket`);
    const videoPlayer = document.querySelector('#video-player');
    const warning = document.querySelector('#warning');


    videoPlayer.onended = () => {
        console.log('Video done, requesting next one...');
        webSocket.send('next video lol');
    };

    webSocket.onopen = () => {
        warning.style.visibility = 'hidden';
        webSocket.send('next video lol');
    };

    webSocket.onmessage = message => {
        console.log(`Received next video "${message.data}".`);
        videoPlayer.src = `/video/${message.data}`;
        videoPlayer.play();
    };

    webSocket.onclose = webSocket.onerror = e => {
        console.error('WebSocket was closed:', e);
        warning.style.visibility = 'visible';
    };
};