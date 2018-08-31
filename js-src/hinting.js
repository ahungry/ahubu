// This will control the hinting on page

var hints = 'abcdefghijklmnopqrstuvwxyz1234567890'.split('')

function hint_span (c) {
  return '<span class="ahubu-hint" style="display:none;font-style:none !important; font-size:16px !important; position:absolute;background:rgba(0,0,0,.7);color:#fa0;font-weight:bold;padding:4px;left:10;top:-8;font-family:Iosevka,monospace;">' + c + '</span>'
}

var hinted = false
var hint_map = {}

function hinting () {
  if (hinted) return

  var links = document.getElementsByTagName("a")

  for (var i = 0; i < links.length; i++) {
    var hint = i > hints.length ? '' : hints[i]
    links[i].innerHTML = hint_span(hint) + links[i].innerHTML
    hint_map[hint] = links[i].href
  }

  hinted = true
}

setTimeout(hinting, 100)

function hinting_set (display) {
  var links = document.getElementsByClassName("ahubu-hint")

  for (var i = 0; i < links.length; i++) {
    links[i].style.display = display
  }
}

var hint_mode = false

function find_hint (c) {
  hint_mode = false

  var url = hint_map[c.toLowerCase()]
  window.location.assign(url)
  /*
  var links = document.getElementsByClassName("ahubu-hint")

  for (var i = 0; i < links.length; i++) {
    if (links[i].innerHTML.toLowerCase() === c.toLowerCase()) {
      var href = links[i].parentNode  //.getElementsByTagName('a')//[0].href
      window.location.assign(href)
    }
  }
  */
}

document.addEventListener('keyup', (e) => {
  if (!hint_mode) return

  find_hint(String.fromCharCode(e.keyCode))
})

var hinting_on = () => {
  hinting_set('initial')
  hint_mode = true
  setTimeout(() => { hint_mode = true }, 10)
}

var hinting_off = () => {
  hinting_set('none')
  hint_mode = false
}
