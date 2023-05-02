function verifyVerification(id) {
	$.ajax({
		type: "POST",
		url: "/admin/user-verification/verify/" + id,
		success: function() {
			$("#" + id).hide();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function deleteVerification(id) {
	$.ajax({
		type: "POST",
		url: "/admin/user-verification/delete/" + id,
		success: function() {
			$("#" + id).hide();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}