// document.addEventListener('touchstart', onTouchStart, {passive: true});

// window.setInterval("reloadLogbookFrame();", 5000);
window.setInterval("changeDetect();", 1000);
window.setInterval("isRunning();", 2000);

function httpGetAsync(theUrl, callback) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() { 
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
            callback(xmlHttp.responseText);
    }
    xmlHttp.open("GET", theUrl, true); // true for asynchronous 
    xmlHttp.send(null);
}

function reloadLogbookFrame() {
    logbook.location.reload(1);
    setTimeout(function () {scrollToEnd();}, 1000);
}

function changeDetect() {
    var someChange = false;

    var settings = document.getElementById("settings");
    if (settings.value != settings.defaultValue) {
        someChange = true;
        settings.classList.toggle("changed", true);
    }
    else {
        settings.classList.toggle("changed", false);
    }

    var autoStart = document.getElementById("autoStart");
    if (autoStart.checked != autoStart.defaultChecked) {
        someChange = true;
        autoStart.parentNode.classList.toggle("changedOverBackground", true);
    }
    else {
        autoStart.parentNode.classList.toggle("changedOverBackground", false);
    }

    var save = document.getElementById("save");
    save.classList.toggle("buttonChanged", someChange);
}

function scrollToEnd() { 
    var frame = window.frames.logbook; 
    frame.scrollTo(0, 10000000000000000); 
} 


function isRunning() {
    httpGetAsync(window.location.href + "state", updateStartStopState);
}

function updateStartStopState(state) {
    var isStarted = state === true || state === "true";
    var startStop = document.getElementById("startStop");
    var startStopLed = document.getElementById("startStopLed");

    var newValue = isStarted ? "Stop" : "Start";
    var isTheSame = startStop.value === newValue;
    startStop.value = newValue;

    startStopLed.classList.toggle("led-yellow", false);
    startStopLed.classList.toggle("led-green", isStarted);
    startStopLed.classList.toggle("led-red", !isStarted);

    // console.log(" '" + startStop.value + " '" + newValue + "'  " + isTheSame);
    if (!isTheSame) {
        console.log("RELOAD: " + window.location.pathname + window.location.hash);

        window.location.assign(window.location.pathname + window.location.hash);
    }
}