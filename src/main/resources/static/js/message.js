function viewMessageDetail(e, id) {
	
    var senderElement = e.target;
    if($(e.target).is("img")) {
    	e.stopPropagation();
    	return true;
    }
	
	window.open("/chats/" + id ,"_self");
}