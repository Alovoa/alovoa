$(function() {
	$("#form").submit(function(e) {
		e.preventDefault();

		if (!checkPassword()) {
			return;
		}

		let email = $("#email").val();
		let password = $("#password").val();
		let token = $("#token").val();
		let data = {};
		data.email = email;
		data.password = password;
		data.token = token;

		$.ajax({
			type: "POST",
			url: "/password/change",
			data: JSON.stringify(data),
			contentType: "application/json",
			success: function(e) {
				location.href = "/?password-change-success";
			},
			error: function(e) {
				alert(getGenericErrorText());
			}
		});

	});
});
