window.onload = () => {
    const webSocket = new WebSocket(`ws://localhost:${serverPort}/socket`);
    const videoPlayer = document.querySelector('#video-player');
    const warning = document.querySelector('#warning');

    // on certain video files, for whatever reason, Chromium does not call the ended event at the end, only the pause event
    // this will cause a normal pause event, if ever needed, to not work
    videoPlayer.onended = videoPlayer.onpause = () => {
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
    };

    webSocket.onclose = webSocket.onerror = e => {
        console.error('WebSocket was closed:', e);
        warning.style.visibility = 'visible';
    };
};