//TODO
var messageMaxLength = 255;
var reloadInterval = 10000;

$(document).ready(function() {
	reloadMessages(true);

	setInterval(reloadMessages, reloadInterval);

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
	$.ajax({
		type : "POST",
		url : "/message/send/" + getConvoId(),
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		contentType : "text/plain",
		data : $("#message-send-input").val(),
		success : function() {
			$("#message-send-input").val("");
			reloadMessages(true);
		},
		error : function(e) {
			console.log(e);
			reloadMessages(true);
		}
	});
}

function reloadMessages(scrollToBottom) {
	$("#messages-div").load(
			"/message/get-messages/" + getConvoId(),
			function() {
				if (scrollToBottom) {
					$("#messages-div").scrollTop(
							$("#messages-div")[0].scrollHeight);
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