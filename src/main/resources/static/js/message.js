function viewMessageDetail(e, id) {
	
    var senderElement = e.target;
    if($(e.target).is("img")) {
    	e.stopPropagation();
    	return true;
    }
	
	window.open("/chats/" + id ,"_self");
}

$('.profile-pic').click(function(event){
    event.stopPropagation();
	let val = $(event.target).attr("value");
	viewProfile(val);
});

function viewProfile(idEnc) {
	let url = 'profile/view/' + idEnc;
	window.open(url, '_blank').focus();
}