// This can disable all the inputs (to avoid eating user key commands that pass through)
function set_forms (state) {
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
}

const disable_form = () => set_forms(true)
const enable_form = () => set_forms(false)

disable_form()
