if(window!=window.top) {
    window.top.location.href='http://juick.com/post';
}

function clearLocation() {
    document.getElementById("location").innerHTML='<a href="#" onclick="addLocation()">Choose</a>';
    document.getElementById("locationclear").style.display="none";
    document.getElementById("geomap").style.display="none";
    document.forms["postmsg"].place_id.value=0;
}

function addLocation() {
    document.getElementById("location").innerHTML="?";
    document.getElementById("locationclear").style.display="inline";
    document.getElementById("geomap").style.display="block";
    if(!map) {
        mapInit('show=places',1,1);
        GEvent.addListener(map,"click", function(overlay) {
            if(overlay instanceof GMarker) {
                document.getElementById("location").innerHTML='<a href="/places/'+overlay.param+'">'+overlay.getTitle()+'</a>';
                document.forms["postmsg"].place_id.value=overlay.param;
            }
        });
    }
}

function addTag(tag) {
    document.forms["postmsg"].body.value='*'+tag+' '+document.forms["postmsg"].body.value;
    return false;
}

function webcamShow() {
    swfobject.embedSWF('http://juick.com/_webcam.swf','webcam','320','280','9.0.115','false',null,null,null);
}

function webcamImage(hash) {
    document.getElementById("webcamwrap").innerHTML='<div id="webcam"></div>';
    document.getElementById("attachmentfile").style.display="none";
    document.getElementById("attachmentwebcam").style.display="inline";
    document.forms["postmsg"].webcam.value=hash;
}

function clearAttachment() {
    document.getElementById("attachmentfile").style.display="inline";
    document.getElementById("attachmentwebcam").style.display="none";
    document.forms["postmsg"].webcam.value="";
}

$(document).ready(function() {
    clearLocation();
    clearAttachment();
    $("textarea")[0].focus();
});

$(window).unload(GUnload);
