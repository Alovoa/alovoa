function rootResize() {
	document.querySelector(':root').style
		.setProperty('--vh', window.innerHeight / 100 + 'px');
}

document.addEventListener("DOMContentLoaded", function(){
	document.querySelector(':root').style
	.setProperty('--vh', window.innerHeight / 100 + 'px');

	window.addEventListener('resize', rootResize);
});

function alert(text, position) {
	let duration = 2000;
	if(text.length < 40) {
		duration = 1500;
	} else if(text.length > 70) {
		duration = 3000;
	} else if (text.length > 100) {
		duration = 4000;
	}
	bulmaToast.toast({
		message: text,
		type: 'is-info',
		position: position ? position : 'bottom-center',
		closeOnClick: false,
		pauseOnHover: false,
		duration: duration,
		animate: { in: 'fadeIn', out: 'fadeOut' }
	})
}

window.onload = function(){
	let div = document.getElementById("preloader-parent")
	if(div) {
		div.style.display = "none"; 
	}
};
