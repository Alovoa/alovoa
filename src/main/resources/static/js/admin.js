function hideContact(id) {
	$.ajax({
		type: "POST",
		url: "/admin/contact/hide/" + id,
		success: function() {
			$("#contact" + id).hide();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function viewProfile(uuid) {
	let url = '/admin/profile/view/' + uuid;
	window.open(url, '_blank').focus();
}

function viewProfileMedia(uuid) {
	let url = '/admin/profile/view/' + uuid + '/media';
	window.open(url, '_blank').focus();
}

function deleteReport(id, idReal) {
	$.ajax({
		type: "POST",
		url: "/admin/report/delete/" + id,
		success: function() {
			$("#report" + id).hide();
			$(".user" + idReal).hide();
		},
		error: function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

function removeDescription(id) {
	if (confirm("Remove description??")) {
		$.ajax({
			type: "POST",
			url: "/admin/remove-description/" + id,
			success: function() {
				alert(getText("success.submit.generic"));
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

function removeImages(id) {
	if (confirm("Remove all images?")) {
		$.ajax({
			type: "POST",
			url: "/admin/remove-images/" + id,
			success: function() {
				alert(getText("success.submit.generic"));
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

function banUser(id, idReal) {
	if (confirm(getText("admin.ban-user.confirm"))) {
		$.ajax({
			type: "POST",
			url: "/admin/ban-user/" + id,
			success: function() {
				$(".user" + idReal).hide();
			},
			error: function(e) {
				console.log(e);
				alert(getGenericErrorText());
			}
		});
	}
}

$("#mail-single-form").submit(
	function(e) {
		e.preventDefault();

		var actionUrl = e.currentTarget.action;
		let formdata = $("#mail-single-form").serializeArray().reduce(
			function(a, x) {
				a[x.name] = x.value;
				return a;
			}, {});

		$.ajax({
			url: actionUrl,
			type: 'POST',
			data: JSON.stringify(formdata),
			contentType: "application/json",
			success: function(data) {
				$("#mail-single-form")[0].reset();
				alert(getText("success.submit.generic"));
			},
			error: function(e) {
				console.log(e);
				alert(e.responseText);
			}
		});

	});


$("#mail-all-form").submit(
	function(e) {
		e.preventDefault();

		if (confirm(getText("admin.mail.send-all.confirm"))) {

			var actionUrl = e.currentTarget.action;
			let formdata = $("#mail-all-form").serializeArray().reduce(
				function(a, x) {
					a[x.name] = x.value;
					return a;
				}, {});

			$.ajax({
				url: actionUrl,
				type: 'POST',
				data: JSON.stringify(formdata),
				contentType: "application/json",
				success: function(data) {
					$("#mail-all-form")[0].reset();
					alert(getText("success.submit.generic"));
				},
				error: function(e) {
					console.log(e);
					alert(e.responseText);
				}
			});

		}

	});

$("#delete-account-form").submit(
	function(e) {
		e.preventDefault();

		var actionUrl = e.currentTarget.action;
		let formdata = $("#delete-account-form").serializeArray().reduce(
			function(a, x) {
				a[x.name] = x.value;
				return a;
			}, {});

		$.ajax({
			url: actionUrl,
			type: 'POST',
			data: JSON.stringify(formdata),
			contentType: "application/json",
			success: function(data) {
				$("#delete-account-form")[0].reset();
				alert(getText("success.submit.generic"));
			},
			error: function(e) {
				console.log(e);
				alert(e.responseText);
			}
		});

	});

$("#user-exists-form").submit(
	function(e) {
		e.preventDefault();

		var actionUrl = e.currentTarget.action + encodeURIComponent($("#user-exists-email").val());

		$.ajax({
			url: actionUrl,
			type: 'POST',
			success: function(data) {
				$("#user-exists-form")[0].reset();
				if (data) {
					alert("User exists");
				}
				else {
					alert("User doesn't exist");
				}
			},
			error: function(e) {
				console.log(e);
				alert(e.responseText);
			}
		});

	});

$("#donation-add-form").submit(
	function(e) {
		e.preventDefault();

		var actionUrl = e.currentTarget.action + encodeURIComponent($("#donation-add-email").val()) + "/" + $("#donation-add-amount").val();

		$.ajax({
			url: actionUrl,
			type: 'POST',
			success: function(data) {
				$("#donation-add-form")[0].reset();
				alert(getText("success.submit.generic"));
			},
			error: function(e) {
				console.log(e);
				alert(e.responseText);
			}
		});

	});


