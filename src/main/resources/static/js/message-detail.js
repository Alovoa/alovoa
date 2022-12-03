//TODO
var messageMaxLength = 200;
var reloadInterval = 3000;

$(document).ready(function() {
	
	getUpdates();
	showLoader();
	reloadMessages(1);

	setInterval(reloadMessages, reloadInterval, 0);

	$('#message-send-input').keyup(function(e) {
		if (e.keyCode == 13) {
			sendMessage();
		}
	});

	$('#message-send-input').on('keyup paste', function() {
		let data = $('#message-send-input').val();
		var maxlength = messageMaxLength;
		var currentLength = $(this).val().length;

		if (currentLength >= maxlength) {
			
			alert(getText("message.js.error.max-length"));
			$('#message-send-input').val(data.substring(0, maxlength));
		}
	});
});

function sendMessage() {
	
	let data = $("#message-send-input").val();
	
	if(data) {
		$("#message-send-input").val("");
		$.ajax({
			type : "POST",
			url : "/message/send/" + getConvoId(),
			contentType : "text/plain",
			data : data,
			success : function() {
				reloadMessages(0);
			},
			error : function(e) {
				console.log(e);
			}
		});
	}
}

function reloadMessages(first) {
	$.ajax({
		type : "GET",
		url : "/message/get-messages/" + getConvoId() + "/" + first,
		contentType : "text/plain",
		success : function(res) {	
			if(res != "<div></div>") {
				$("#messages-div").html(res);
				$("#messages-div").scrollTop($("#messages-div")[0].scrollHeight);
			}
			if(first) {
				hideLoader();
			}
		},
		error : function(e) {
			console.log(e);
			if(first) {
				hideLoader();
			}
		}
	});
}

function getConvoId() {
	return $("#convo-id").val();
}


function startVideoChat() {
	var answer = window.confirm(getText("message.js.video.confirm"))
	if (answer) {
	    let rand = randomString();
	    let url = "https://meet.jit.si/" + rand;
	    let link = url;
	    $("#message-send-input").val(link);
	    sendMessage();
	    window.open(url);
	}
}

//https://stackoverflow.com/a/10727155
function randomString() {
	const length = 32;
	const chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    var result = '';
    for (var i = length; i > 0; --i) result += chars[Math.floor(Math.random() * chars.length)];
    return result;
}