try {
  // OVERLAY
  var Overlay = {
    show () {
      document.getElementById('ab-ob').style.display = 'block'
    },

    hide () {
      document.getElementById('ab-ob').style.display = 'none'
    },

    create () {
      const ob = document.createElement('div')

      ob.style.display = 'none'
      ob.style.backgroundColor = 'rgba(0,0,0,.5)'
      ob.style.color = 'lime'
      ob.style.fontFamily = 'Iosevka, monospace'
      ob.style.fontSize = '40px'
      ob.style.fontWeight = 'bold'
      ob.style.position = 'fixed'
      ob.style.top = 0
      ob.style.left = 0
      ob.style.height = '100%'
      ob.style.padding = '30px'
      ob.style.width = '100%'
      ob.id = 'ab-ob'
      ob.zIndex = '8888'

      return ob
    },
  }
  document.body.appendChild(Overlay.create())
  Overlay.hide()
  // OVERLAY

  // DISABLE FORMS
  // This can disable all the inputs (to avoid eating user key commands that pass through)
  var Form = {
    toggle (state) {
      state = !state
      var el = undefined

      var selects = document.getElementsByTagName('select')

      for (var i = 0; i < selects.length; i++) {
        selects[i].disabled = state
        selects[i].blur()
      }

      var textareas = document.getElementsByTagName('textarea')

      for (var i = 0; i < textareas.length; i++) {
        textareas[i].disabled = state;
        textareas[i].blur()
      }

      var buttons = document.getElementsByTagName('button')

      for (var i = 0; i < buttons.length; i++) {
        buttons[i].disabled = state
        buttons[i].blur()
      }

      var inputs = document.getElementsByTagName('input')

      for (var i = inputs.length - 1; i >= 0; i--) {
        inputs[i].disabled = state
        inputs[i].blur()
        el = inputs[i]
      }

      if (false === state) {
        // Focus the first form on page
        setTimeout(() => { el.focus() }, 100)
      }
    },

    disable () {
      Form.toggle(false)
    },

    enable () {
      Form.toggle(true)
    },
  }

  Form.disable()
  // END FORMS

  // HINTING
  // This will control the hinting on page
  var Hinting = {
    map: {},
    mode: false,
    hints: 'abcdefghijklmnopqrstuvwxyz1234567890'.split(''),

    get_hint (c) {
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
    },

    on () {
      Hinting.map = {}
      var links = document.getElementsByTagName('a')

      for (var i = 0; i < links.length; i++) {
        if (i > Hinting.hints.length) continue
        var hint = Hinting.hints[i]
        var parent = links[i]

        // This could maybe break some floating links or something...
        // TODO: Maybe check existing position setting is not absolute first.
        parent.style.position = 'relative'

        var h = Hinting.get_hint(hint)
        parent.appendChild(h)

        Hinting.map[hint] = h
      }

      setTimeout(() => { Hinting.mode = true }, 200)
    },

    off (display) {
      Object.keys(Hinting.map).map((k) => {
        Hinting.map[k].remove()
      })

      Hinting.mode = false
    },

    // Simulate an event
    eventFire (el, etype) {
      if (el.fireEvent) {
        el.fireEvent('on' + etype)
      } else {
        var evObj = document.createEvent('Events')
        evObj.initEvent(etype, true, false)
        el.dispatchEvent(evObj)
      }
    },

    find (c, retries = 0) {
      Hinting.mode = false

      var el = Hinting.map[c.toLowerCase()]

      if (undefined === el && retries < 2) {
        Hinting.off()
        Hinting.on()

        return Hinting.find(c, ++retries)
      }

      Hinting.eventFire(el, 'click')

      setTimeout(() => {
        Hinting.off()
        Overlay.hide()
      }, 50)
    },

    bind () {
      document.addEventListener('keyup', (e) => {
        if (false === Hinting.mode) return

        Hinting.find(String.fromCharCode(e.keyCode))
      })
    }
  }

  Hinting.bind()
  // END HINTING

} catch (e) {
  alert(e.toString())
}
