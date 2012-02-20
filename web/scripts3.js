function inlinevideo(mid) {
    var flashvars={
        file:'http://i.juick.com/video/'+mid+'.mp4',
        image:'http://i.juick.com/thumbs/'+mid+'.jpg',
        skin:'http://static.juick.com/glow.zip'
    };
    var params={
        allowfullscreen:'true'
    };
    swfobject.embedSWF('http://static.juick.com/player.swf','video-'+mid,'640','390','9.0.115','false',flashvars,params,null);
}

function postformListener(formEl,ev) {
    if(ev.ctrlKey && (ev.keyCode==10 || ev.keyCode==13)) formEl.submit();
}

function showMoreReplies(id) {
    $('#'+id+' .msg-comments').hide();

    var replies=$('#replies>li');
    var flagshow=0;
    for(var i=0; i<replies.length; i++) {
        if(flagshow==1) {
            if(replies[i].style.display=="none") {
                replies[i].style.display="block";
            } else {
                break;
            }
        }
        if(replies[i].id==id) {
            flagshow=1;
        }
    }
    return false;
}

function showCommentForm(mid,rid) {
    if($('#replies #'+rid+' textarea').length==0) {
        var c=$('#replies #'+rid+' .msg-comment');
        c.wrap('<form action="/post" method="POST" enctype="multipart/form-data"/>');
        c.before('<input type="hidden" name="mid" value="'+mid+'"/><input type="hidden" name="rid" value="'+rid+'"/>');
        c.append('<textarea name="body" rows="1" class="reply" placeholder="Add a comment..." onkeypress="postformListener(this.form,event)"></textarea><input type="submit" value="OK"/>');
    }
//    $('#replies #'+rid+' .msg-links').hide();
    $('#replies #'+rid+' .msg-comment').show();
    $('#replies #'+rid+' textarea')[0].focus();
    $('#replies #'+rid+' textarea').autoResize({
        extraSpace: 0,
        minHeight: 1
    });
    return false;
}

$(document).ready(function() {
    $('textarea.reply').autoResize({
        extraSpace: 0,
        minHeight: 1
    });
});
