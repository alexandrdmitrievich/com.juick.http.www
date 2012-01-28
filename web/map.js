var map;
var mc;
var icon=new GIcon(G_DEFAULT_ICON,"http://maps.google.com/mapfiles/marker_orange.png");

function mapInit() {
    var lat=getHashVar("lat");
    var lon=getHashVar("lon");
    var zoom=getHashVar("zoom");
    if(!lat || !lon || !zoom) {
        lat=readCookie("lat");
        lon=readCookie("lon");
        zoom=readCookie("zoom");
        if(!lat || !lon || !zoom) {
            lat=30;
            lon=0;
            zoom=2;
        }
        else {
            lat=parseFloat(lat);
            lon=parseFloat(lon);
            zoom=parseInt(zoom);
        }
    } else {
        lat=parseFloat(lat);
        lon=parseFloat(lon);
        zoom=parseInt(zoom);
    }

    map=new GMap2(document.getElementById("geomap"));
    map.setCenter(new GLatLng(lat,lon),zoom,G_HYBRID_MAP);
    map.addControl(new GMapTypeControl());
    map.enableScrollWheelZoom();
    map.addControl(new GLargeMapControl(),new GControlPosition(G_ANCHOR_TOP_LEFT, new GSize(-10,-60)));

    mc=new MarkerClusterer(map,null,{
        gridSize:40,
        maxZoom:15
    });
  
    GEvent.addListener(map,"moveend",mapLoadMarkers);
    GEvent.addListener(map,"zoomend",mapLoadMarkers);

    mapLoadMarkers();
}

function mapLoadMarkers() {
    var mapcenter=map.getCenter();
    var lat=Math.round(mapcenter.lat()*100000)/100000;
    var lon=Math.round(mapcenter.lng()*100000)/100000;
    window.location.hash.replace("#lat="+lat+"&lon="+lon+"&zoom="+map.getZoom());
    writeCookie("lat",lat,365,"/map");
    writeCookie("lon",lon,365,"/map");
    writeCookie("zoom",map.getZoom(),365,"/map");

    var bounds=map.getBounds();
    var swlat=bounds.getSouthWest().lat();
    if(swlat<-90) swlat=-90;
    var swlng=bounds.getSouthWest().lng();
    if(swlng<-180) swlng=-180;
    
    var nelat=bounds.getNorthEast().lat();
    if(nelat>90) nelat=90;
    var nelng=bounds.getNorthEast().lng();
    if(nelng>180) nelng=180;

    if(nelng<swlng) {
        var tmp=nelng;
        nelng=swlng;
        swlng=tmp;
    }

    var nodes=document.getElementsByClassName("loadScript");
    for(var i=0; i<nodes.length; i++)
        nodes[i].parentNode.removeChild(nodes[i]);
    loadScript("http://api.juick.com/places?south="+swlat+"&west="+swlng+"&north="+nelat+"&east="+nelng+"&count=75&callback=mapParsePlaces");
    loadScript("http://api.juick.com/messages?south="+swlat+"&west="+swlng+"&north="+nelat+"&east="+nelng+"&callback=mapParseMessages");
}

function loadScript(src) {
    var scripttag=document.createElement("script");
    scripttag.setAttribute("type","text/javascript");
    scripttag.setAttribute("src",src);
    scripttag.setAttribute("class","loadScript");
    document.getElementsByTagName("head")[0].appendChild(scripttag); 
}

function mapParsePlaces(json) {
    var places=document.getElementById("places");
    while(places.hasChildNodes()) places.removeChild(places.lastChild);
    var markers=[];
    for(var i=0; i<json.length; i++) {
        markers.push(
            createMarker(
                json[i].pid,
                new GLatLng(parseFloat(json[i].lat),parseFloat(json[i].lon)),
                json[i].name,
                "http://juick.com/places/"+json[i].pid,
                icon
                )
            );
        if(i<10) {
            var li=document.createElement("li");
            li.innerHTML='<li><a href="/places/'+json[i].pid+'">'+json[i].name+'</a></li>';
            places.appendChild(li);
        }
    }
    mc.clearMarkers();
    mc.addMarkers(markers);
}

function mapParseMessages(json) {
    var msgs=document.getElementById("messages");
    while(msgs.hasChildNodes()) msgs.removeChild(msgs.lastChild);
    for(var i=0; i<json.length; i++) {
        var replies=json[i].replies;
        if(!replies) replies=0;
        var ihtml='<div class="msg"><big><a href="/'+json[i].user.uname+'/">@'+json[i].user.uname+'</a>:';
        if(json[i].tags)
            for(var n=0; n<json[i].tags.length; n++)
                ihtml+=' <a href="/'+json[i].user.uname+'/?tag='+json[i].tags[n]+'">*'+json[i].tags[n]+'</a>';
        ihtml+='</big><div class="msgtxt">';
        if(json[i].location)
            ihtml+='<b>Location:</b> <a href="/places/'+json[i].location.place_id+'">'+json[i].location.name+'</a><br/>';
        if(json[i].photo)
            ihtml+='<b>Attachment:</b> <a href="'+json[i].photo.medium+'">Photo</a><br/>';
        if(json[i].video)
            ihtml+='<b>Attachment:</b> <a href="'+json[i].video.mp4+'">Video</a><br/>';
        ihtml+=json[i].body+'</div><div class="msgbottom"><div class="msgnum"><a href="/'+json[i].user.uname+'/'+json[i].mid+'">#'+json[i].mid+'</a></div><div class="msginfo"><a href="/'+json[i].user.uname+'/'+json[i].mid+'">replies: '+replies+'</a></div></div></div>';

        var li=document.createElement("li");
        li.className='liav';
        li.style.backgroundImage='url(http://i.juick.com/as/'+json[i].user.uid+'.png)';
        li.innerHTML=ihtml;
        msgs.appendChild(li);
    }
}

function createMarker(id,latlng,title,href,icon) {
    var marker=new GMarker(latlng,{
        'icon':icon,
        'title':title
    });
    marker.param=id;
    if(href && href!="")
        GEvent.addListener(marker,"click",function(ll) {
            var txt='<a href="'+href+'">'+title+'</a>';
            map.openInfoWindowHtml(ll,txt);
        });
    return marker;
}

function getHashVar(variable) {
    var query=window.location.hash.substring(1);
    var vars=query.split("&");
    for(var i=0; i<vars.length; i++) {
        var pair=vars[i].split("=");
        if(pair[0]==variable) return pair[1];
    }
    return null;
}

function writeCookie(name,value,days,path) {
    var expires;
    if(days) {
        var date=new Date();
        date.setTime(date.getTime()+(days*24*60*60*1000));
        expires="; expires="+date.toGMTString();
    } else expires="";
    if(!path) path="/";
    document.cookie=name+"="+value+expires+"; path="+path;
}

function readCookie(name) {
    var nameEQ=name+"=";
    var ca=document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
        var c=ca[i];
        while(c.charAt(0)==' ') c=c.substring(1,c.length);
        if(c.indexOf(nameEQ)==0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}
