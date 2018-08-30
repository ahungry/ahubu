// This will control the hinting on page

function hinting() {
  var links = document.getElementsByTagName("a");

  for (var i = 0; i < links.length; i++) {
    links[i].innerHTML = '(n)' + links[i].innerHTML
  }
}

hinting()
alert('yay')
