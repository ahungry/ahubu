// This will control the hinting on page

const hints = 'abcdefghijklmnopqrstuvwxyz1234567890'.split('')

function hint_span(c) {
  return '<span class="ahubu-hint" style="display:none;font-style:none !important; font-size:16px !important; position:absolute;background:rgba(0,0,0,.7);color:#fa0;font-weight:bold;padding:4px;left:10;top:-8;font-family:Iosevka,monospace;">' + c + '</span>'
}

const hinted = false

function hinting() {
  if (hinted) return

  var links = document.getElementsByTagName("a")

  for (var i = 0; i < links.length; i++) {
    const hint = hints[i]
    links[i].innerHTML = hint_span(hint) + links[i].innerHTML
  }

  hinted = true
}

setTimeout(hinting, 500)

function hinting_set(display) {
  var links = document.getElementsByClassName("ahubu-hint")

  for (var i = 0; i < links.length; i++) {
    links[i].style.display = display
  }
}

function find_hint(c) {
  var links = document.getElementsByClassName("ahubu-hint")

  for (var i = 0; i < links.length; i++) {
    if (links[i].innerHTML.toLowerCase() === c.toLowerCase()) {
      var href = links[i].parentNode  //.getElementsByTagName('a')//[0].href
      window.location.assign(href)
    }
  }
}

let hint_mode = false

document.addEventListener('keyup', (e) => {
  if (!hint_mode) return

  find_hint(String.fromCharCode(e.keyCode))
})

const hinting_on = () => {
  hinting_set('initial')

  setTimeout(() => { hint_mode = true }, 100)
}

const hinting_off = () => {
  hinting_set('none')
  hint_mode = false
}
