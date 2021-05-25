function hideContact(id) {
	$.ajax({
				type : "POST",
				url : "/admin/contact/hide/" + id,
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				success : function() {
					$( "#contact" + id ).hide();
				},
				error : function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
		});
}

function viewProfile(idEnc) {
	let url = '/profile/view/' + idEnc;
	window.open(url, '_blank').focus();
}

function deleteReport(id) {
	$.ajax({
				type : "POST",
				url : "/admin/report/delete/" + id,
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				success : function() {
					$( "#report" + id ).hide();
				},
				error : function(e) {
					console.log(e);
					alert(getGenericErrorText());
				}
		});
}

function banUser(id, idReal) {
	if(confirm(getText("admin.ban-user.confirm"))) {
		$.ajax({
					type : "POST",
					url : "/admin/ban-user/" + id,
					headers : {
						"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
					},
					success : function() {
						$( ".user" + idReal ).hide();
					},
					error : function(e) {
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
			url : actionUrl,
			headers : {
				"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
			},
			type : 'POST',
			data : JSON.stringify(formdata),
			contentType : "application/json",
			success : function(data) {
				$("#mail-single-form")[0].reset();
				alert(getText("success.submit.generic"));
			},
			error : function(e) {
				console.log(e);
				alert(e.responseText);
			}
		});

	});


$("#mail-all-form").submit(
	function(e) {
		e.preventDefault();
		
		if(confirm(getText("admin.mail.send-all.confirm"))) {

			var actionUrl = e.currentTarget.action;
			let formdata = $("#mail-all-form").serializeArray().reduce(
					function(a, x) {
						a[x.name] = x.value;
						return a;
					}, {});
	
			$.ajax({
				url : actionUrl,
				headers : {
					"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
				},
				type : 'POST',
				data : JSON.stringify(formdata),
				contentType : "application/json",
				success : function(data) {
					$("#mail-all-form")[0].reset();
					alert(getText("success.submit.generic"));
				},
				error : function(e) {
					console.log(e);
					alert(e.responseText);
				}
			});
		
		}

	});