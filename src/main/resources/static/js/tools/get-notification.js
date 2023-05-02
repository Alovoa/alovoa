//TODO
var getUpdateInterval = 10000;
var getUpdate = setInterval(getUpdates, getUpdateInterval);
var newAlert = false;
var newMessage = false;

const alertTitleText = "(!) ";

function getUpdates() {
	var res = [];
	res.push(getNewAlert());
	res.push(getNewMessage());
	$.when.apply(this, res).done(function() {
		if (newAlert) {
			$("#nav-alerts").addClass("new");
		}		
		if (newMessage) {
			$("#nav-chats").addClass("new");
		}
		if((newAlert || newMessage) && !document.title.includes(alertTitleText)) {
			document.title = alertTitleText + document.title;
		}
		else if(!(newAlert || newMessage) && document.title.includes(alertTitleText)) {
			document.title = document.title.replace(alertTitleText, '');
		}
		
	});
}


document.addEventListener("DOMContentLoaded", function() {
	getNewAlert();
	getNewMessage();
	$.ajax({
		type: "POST",
		url: "/user/post",
		error: function(e) {
			if (e.status == 403) {
				location.reload();
			}
		}
	});
});

function getNewAlert() {
	return $.ajax({
		type: "GET",
		url: "/user/status/new-alert",
		success: function(bool) {
			newAlert = bool;
		},
		error: function(e) {
			console.log(e);
			// clearTimeout(alertTimeout);
			if (e.status == 403) {
				location.reload();
			}
		}
	});
}

function getNewMessage() {
	return $.ajax({
		type: "GET",
		url: "/user/status/new-message",
		success: function(bool) {
			newMessage = bool;
		},
		error: function(e) {
			console.log(e);
			// clearTimeout(messageTimeout);
		}
	});
}
