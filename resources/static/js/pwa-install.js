window.addEventListener('beforeinstallprompt', (e) => {
    e.prompt(); 
    e.userChoice.then((choiceResult) => {
        if (choiceResult.outcome === 'accepted') {
          console.log('Accepted PWA');
        } else {
          console.log('Refused PWA');
        }
        deferredPrompt = null;
      });
  });
});