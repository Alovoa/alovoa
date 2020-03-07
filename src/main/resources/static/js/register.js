//TODO
const minAge = 16;

$(function() {
	
	refreshCaptcha();

	let today = new Date();
	let startDate = new Date(today.setFullYear(today.getFullYear() - minAge));

	// Initialize all input of date type.
	bulmaCalendar.attach('[type="date"]', {
		showHeader : false,
		startDate : startDate,
		maxDate : startDate,
		showClearButton : false,
		showTodayButton : false,
		cancelLabel : '<i class="fa fa-times" style="width: 100%"></i>',
		type : 'date',
		dateFormat : 'YYYY-MM-DD'
	});

	$("#register-form").submit(
			function(e) {
				e.preventDefault();

				var actionUrl = e.currentTarget.action;
				let formdata = $("#register-form").serializeArray().reduce(
						function(a, x) {
							a[x.name] = x.value;
							return a;
						}, {});

				console.log(formdata);
				if (!checkPassword()) {
					return;
				}

				console.log(actionUrl)

				$.ajax({
					url : actionUrl,
					headers : {
						"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
					},
					type : 'POST',
					data : JSON.stringify(formdata),
					contentType : "application/json",
					success : function(data) {
						window.location = "/?confirm-registration";
					},
					error : function() {
						refreshCaptcha();
					}
				});

			});
});
