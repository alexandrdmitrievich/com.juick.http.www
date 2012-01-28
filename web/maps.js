var map;
var mc;
var mapParams;
var mapBounds=null;
var cross=null;
var jcontrol=null;
var crossOnMap=false;

function mapInitAny() {
    map=new GMap2(document.getElementById("geomap"));
    map.setMapType(G_HYBRID_MAP);
    map.addControl(new GMapTypeControl());
    map.enableScrollWheelZoom();
}

function mapInitSimple(lat,lng,iconcolor) {
    mapInitAny();
    map.addControl(new GSmallZoomControl());
    map.setCenter(new GLatLng(lat,lng),15);

    var icon=new GIcon(G_DEFAULT_ICON);
    if(iconcolor && iconcolor!='red') icon.image="http://maps.google.com/mapfiles/marker_"+iconcolor+".png";

    map.addOverlay(new GMarker(new GLatLng(lat,lng),{
        'icon':icon,
        'clickable':false
    }));
}

function mapInit(params,autozoom,place) {
    mapParams=params;

    mapInitAny();
    map.addControl(new GLargeMapControl(),new GControlPosition(G_ANCHOR_TOP_LEFT, new GSize(-10,-60)));
    if(place) {
        jcontrol=new JuickControl();
        cross=new GScreenOverlay('http://static.juick.com/cross.png',
            new GScreenPoint(0.5, .5, 'fraction', 'fraction'),
            new GScreenPoint(9, 9),
            new GScreenSize(19, 19)
            );
    }
    map.setCenter(new GLatLng(30,0),2);

    mc=new MarkerClusterer(map,null,{
        gridSize:40,
        maxZoom:15
    });
  
    GEvent.addListener(map,"moveend",mapLoadMarkers);
    GEvent.addListener(map,"zoomend",mapLoadMarkers);

    if(autozoom==1 && navigator.geolocation) navigator.geolocation.getCurrentPosition(mapSetCenter,null,{
        timeout:5
    });
    else mapLoadMarkers(autozoom);
}

// call loadMarkers even if getCurrentPosition failed

function mapSetCenter(pos) {
    map.setCenter(new GLatLng(pos.coords.latitude,pos.coords.longitude),11);
    mapLoadMarkers();
}

function mapLoadMarkers(zoomOld) {
    var zoom=map.getZoom();
    if(zoom>14 && cross!=null && !crossOnMap) {
        map.addControl(jcontrol);
        map.addOverlay(cross);
        crossOnMap=true;
    }
    if(zoom<=14 && cross!=null && crossOnMap) {
        map.removeControl(jcontrol);
        map.removeOverlay(cross);
        crossOnMap=false;
    }
  
    var bounds=map.getBounds();
    if(mapBounds==null || !mapBounds.containsBounds(bounds) || zoomOld>0) {
        var span=bounds.toSpan();

        var swlat=bounds.getSouthWest().lat()-span.lat()/3;
        if(swlat<-90) swlat=-90;
        var swlng=bounds.getSouthWest().lng()-span.lng()/3;
        if(swlng<-180) swlng=-180;
    
        var nelat=bounds.getNorthEast().lat()+span.lat()/3;
        if(nelat>90) nelat=90;
        var nelng=bounds.getNorthEast().lng()+span.lng()/3;
        if(nelng>180) nelng=180;

        mapBounds=new GLatLngBounds(new GLatLng(swlat,swlng),new GLatLng(nelat,nelng));

        var q="/_mapxml?"+mapParams+"&south="+swlat+"&west="+swlng+"&north="+nelat+"&east="+nelng;
        GDownloadUrl(q,function(data) {
            var xmlmarkers=GXml.parse(data).documentElement.getElementsByTagName("marker");
            var markers=[];
            var mbounds=new GLatLngBounds();
            var icon;
            var iconcolor="null";
            for(var i=0; i<xmlmarkers.length; i++) {
                var latlng=new GLatLng(parseFloat(xmlmarkers[i].getAttribute("lat")),parseFloat(xmlmarkers[i].getAttribute("lon")));
                var iconcolornew=xmlmarkers[i].getAttribute("color");
                if(iconcolor!=iconcolornew) {
                    iconcolor=iconcolornew;
                    icon=new GIcon(G_DEFAULT_ICON);
                    if(iconcolor!="" && iconcolor!='red') icon.image="http://maps.google.com/mapfiles/marker_"+iconcolor+".png";
                }
                markers.push(
                    createMarker(
                        xmlmarkers[i].getAttribute("param"),
                        latlng,
                        xmlmarkers[i].getAttribute("title"),
                        xmlmarkers[i].getAttribute("href"),
                        icon
                        )
                    );
                if(zoomOld==-1) mbounds.extend(latlng);
            }
            mc.clearMarkers();
            mc.addMarkers(markers);
            if(zoomOld==-1) {
                var zoom=map.getBoundsZoomLevel(mbounds)-1;
                if(zoom>14) zoom=14;
                else if(zoom<1) zoom=1;
                map.setCenter(mbounds.getCenter(),zoom);
            }
        });
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

//

function JuickControl() {}
JuickControl.prototype = new GControl();
JuickControl.prototype.initialize = function(map) {
    var container = document.createElement("div");
  
    var bAddPlace = document.createElement("div");
    this.setButtonStyle_(bAddPlace);
    container.appendChild(bAddPlace);
    bAddPlace.appendChild(document.createTextNode("Add place"));
    GEvent.addDomListener(bAddPlace, "click", this.onAddPlaceClick);
    map.getContainer().appendChild(container);
    return container;
}

JuickControl.prototype.getDefaultPosition = function() {
    return new GControlPosition(G_ANCHOR_TOP_RIGHT, new GSize(7, 35));
}

JuickControl.prototype.setButtonStyle_ = function(button) {
    button.style.color = "#000000";
    button.style.backgroundColor = "white";
    button.style.font = "small Arial";
    button.style.border = "1px solid black";
    button.style.padding = "2px";
    button.style.marginBottom = "5px";
    button.style.textAlign = "center";
    button.style.width = "6em";
    button.style.cursor = "pointer";
}

var htmlAddPlace='<form><p style="width: 440px"><b>Name:</b><br/>\
<input type="text" name="description" maxlength="64" style="width: 400px"/><br/>\
Impersonal description (max 255 symbols):<br/>\
<textarea name="text" rows="5" style="width: 400px"></textarea><br/>\
URL:<br/>\
<input type="text" name="url" maxlength="128" style="width: 400px"/><br/>\
Tags (separated by space, 10 max):\
<input type="text" name="tags" maxlength="255" style="width: 400px"/></p>\
<p style="width: 400px; text-align: right; margin-bottom: 0"><input type="button" value="   Add   " onclick="addPlace(this.form)"/></p>\
</form>';

var placeLatLng;

JuickControl.prototype.onAddPlaceClick = function() {
    placeLatLng=map.getCenter();
    map.removeOverlay(cross);
    map.openInfoWindowHtml(placeLatLng,htmlAddPlace,{
        onCloseFn:function() {
            map.panTo(placeLatLng);
            map.addOverlay(cross);
        }
    });
}

function addPlace(form) {
    var description=form.description.value;
    if(description=='') {
        alert('Enter place name.');
        return;
    }
    var text=form.text.value;
    var url=form.url.value;
    var tags=form.tags.value;
    map.closeInfoWindow();
    GDownloadUrl("/_mapxml",function(data) {
        var xmlmarkers=GXml.parse(data).documentElement.getElementsByTagName("marker");

        var icon=new GIcon(G_DEFAULT_ICON);
        icon.image="http://maps.google.com/mapfiles/marker_orange.png";
        var markers=[];
        markers.push(createMarker(
            xmlmarkers[0].getAttribute("param"),
            placeLatLng,
            description,
            xmlmarkers[0].getAttribute("href"),
            icon
            ));
        mc.addMarkers(markers);
    },'lat='+placeLatLng.lat()+'&lon='+placeLatLng.lng()+'&description='+escape(description)+'&text='+escape(text)+'&url='+escape(url)+'&tags='+escape(tags));
}
