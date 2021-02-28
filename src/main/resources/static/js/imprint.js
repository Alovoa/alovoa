$(function() {
	$("#imprint-form").submit(
			function(e) {
				e.preventDefault();

				var actionUrl = e.currentTarget.action;
				let formdata = $("#imprint-form").serializeArray().reduce(
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
					success : function(e) {
						alert(getText("success.generic"));
						location.reload();
					},
					error : function(e) {
						refreshCaptcha();
						console.log(e);
						alert(e.responseText);
					}
				});

			});
});