$(function() {
	
	$("#delete-account-form").submit(
			function(e) {
				e.preventDefault();

				var actionUrl = e.currentTarget.action;
				let formdata = $("#delete-account-form").serializeArray().reduce(
						function(a, x) {
							a[x.name] = x.value;
							return a;
						}, {});
				
				formdata.confirm = formdata.confirm == "on";
			
				showLoader();
				$.ajax({
					url : actionUrl,
					headers : {
						"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
					},
					type : 'POST',
					data : JSON.stringify(formdata),
					contentType : "application/json",
					success : function(data) {
						window.location = "/?confirm-account-deleted";
						hideLoader();
					},
					error : function(e) {
						console.log(e)
						refreshCaptcha();
						hideLoader();
						alert(getGenericErrorText());
					}
				});

			});
});
