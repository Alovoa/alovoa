$(function() {
	let url = window.location.href;

	if (url.includes("?error")) {
		alert(getGenericErrorText());
	}

	$("#form").submit(function(e) {
		e.preventDefault();
		let email = $("#email").val();
		let password = $("#password").val();
		let token = $("#token").val();
		let data = {};
		data.email = email;
		data.password = password;
		data.token = token;

		$.ajax({
			type : "POST",
			url : "/password/change/",
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			data : JSON.stringify(data),
			contentType : "application/json",
			success : function(e) {
				location.href = "/?password-change-success";
			},
			error : function(e) {
				location.href = "/password/change?error";
			}
		});

	});
});
