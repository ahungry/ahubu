// This will control the hinting on page
var hint_map = {}
var hint_mode = false
var hints = 'abcdefghijklmnopqrstuvwxyz1234567890'.split('')

function get_hint (c) {
  var h = document.createElement('div')

  h.style.backgroundColor = 'rgba(0,0,0,.6)'
  h.style.fontFamily = 'Iosevka, monospace'
  h.style.fontWeight = 'bold'
  h.style.padding = '3px'
  h.style.borderRadius = '3px'
  h.style.color = '#af0'
  h.style.position = 'absolute'
  h.style.left = '0'
  h.style.top = '0'
  h.style.zIndex = '999999999999999999999999999999999999999999999'
  h.innerHTML = c

  return h
}

function hinting_on () {
  hint_map = {}
  var links = document.getElementsByTagName('a')

  for (var i = 0; i < links.length; i++) {
    var hint = i > hints.length ? '' : hints[i]
    var parent = links[i]

    // This could maybe break some floating links or something...
    // TODO: Maybe check existing position setting is not absolute first.
    parent.style.position = 'relative'

    var h = get_hint(hint)
    parent.appendChild(h)

    hint_map[hint] = h
  }

  setTimeout(() => { hint_mode = true }, 200)
}

function hinting_off (display) {
  Object.keys(hint_map).map((k) => {
    hint_map[k].remove()
  })

  hint_mode = false
}

// Simulate an event
function eventFire (el, etype) {
  if (el.fireEvent) {
    el.fireEvent('on' + etype)
  } else {
    var evObj = document.createEvent('Events')
    evObj.initEvent(etype, true, false)
    el.dispatchEvent(evObj)
  }
}

function find_hint (c, retries = 0) {
  hint_mode = false

  var el = hint_map[c.toLowerCase()]

  if (undefined === el && retries < 2) {
    hinting_off()
    hinting_on()

    return find_hint(c, ++retries)
  }

  eventFire(el, 'click')
  setTimeout(() => {
    hinting_off()
  }, 50)
}

document.addEventListener('keyup', (e) => {
  if (!hint_mode) return

  find_hint(String.fromCharCode(e.keyCode))
})
