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