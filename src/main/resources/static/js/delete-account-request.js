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

            showLoader();
            $.ajax({
                url : actionUrl,
                type : 'POST',
                data : JSON.stringify(formdata),
                contentType : "application/json",
                success : function(data) {
                    window.location = "/?account-deletion-requested";
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
