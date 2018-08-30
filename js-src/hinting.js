// This will control the hinting on page

const hints = 'abcdefghijklmnopqrstuvwxyz1234567890'.split('')

function hint_span(c) {
  return '<span class="ahubu-hint" style="font-style:none !important; font-size:16px !important; position:absolute;background:rgba(0,0,0,.7);color:#fa0;font-weight:bold;padding:4px;left:10;top:-8;font-family:Iosevka,monospace;">' + c + '</span>'
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

function hinting_set(display) {
  var links = document.getElementsByClassName("ahubu-hint")

  for (var i = 0; i < links.length; i++) {
    links[i].style.display = display
  }
}

function hinting_off() { hinting_set('none') }
function hinting_on() { hinting_set('block') }

hinting()
hinting_on()
// hinting_set('none')
// hinting_set('block')
