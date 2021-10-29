function rootResize() {
	document.querySelector(':root').style
		.setProperty('--vh', window.innerHeight / 100 + 'px');
}

document.addEventListener("DOMContentLoaded", function(){
	document.querySelector(':root').style
	.setProperty('--vh', window.innerHeight / 100 + 'px');

	window.addEventListener('resize', rootResize);
	
	const swup = new Swup();
});