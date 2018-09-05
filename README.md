# AHUBU (ahungry-browser)

![ahubu](https://github.com/ahungry/ahubu/blob/master/ahubu.png)

## AHUBU Helps Users Be Utilitarian

> A doctrine that the useful is the good and that the determining
> consideration of right conduct should be the usefulness of its
> consequences; specifically : a theory that the aim of action should be
> the largest possible balance of pleasure over pain or the greatest
> happiness of the greatest number .

As such, the browser aims to allow this power to shift to what the
*user* needs and desires (via customization / extensions not just as
sandboxed javascript, but native code that can hook into everything
from DOM events, network requests/content, and arbitrary event
listeners, both in and out of the DOM).

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
**Table of Contents**

- [AHUBU (ahungry-browser)](#ahubu-ahungry-browser)
    - [AHUBU Helps Users Be Utilitarian](#ahubu-helps-users-be-utilitarian)
    - [What is it?](#what-is-it)
        - [Why?](#why)
    - [Installation](#installation)
        - [Java Dependencies](#java-dependencies)
            - [Arch Linux](#arch-linux)
            - [Ubuntu 18.04](#ubuntu-1804)
    - [Usage](#usage)
    - [Options](#options)
        - [Custom keybinds/behavior](#custom-keybindsbehavior)
        - [Network Filtering](#network-filtering)
        - [Hooks](#hooks)
    - [License](#license)

<!-- markdown-toc end -->


## What is it?

A very customizable browser - initial keybindings are a mesh
between Emacs and VIM, as I use Emacs with Evil bindings and that's my
preference.

The closest browsers in default keybindings would be compared to
Firefox or Chrom(e|ium) with vimium/vimperator etc., or one of the
niche webkit browsers such as Lisp Kit or Vimprobable2.

### Why?

Most the good built in keybind browsers (vimprobable2) have outdated
web engines with little maintenance, and the new module system on
Firefox 55+ has weak support for the level of sophistication we need
in handling these key bindings.

## Installation

If you want to run from lein to play with the source and interactive REPL, ensure you have lein installed
(https://leiningen.org/) as well as a JDK that supports JavaFX
(openjdk-8-jdk and openjfx will work on most distros - later versions
of openjdk beyond 9 do not allow side installs of openjfx, nor do most
distros ship with it pre-compiled in).

On Arch Linux, just installing latest openjdk + openjfx worked out of
the box.  On Ubuntu 18.04, special care must be taken to use the Java
version 8 instead of the 11 preview.

### Java Dependencies

#### Arch Linux

Just install openjdk and openjfx

#### Ubuntu 18.04

Install this:

```sh
sudo apt install openjdk-8-jdk openjfx
sudo update-alternatives --config java # Select java 8
```

Run the following (until I am distributing the uberjar to run a standalone):

```
lein deps
lein run -m ahubu.core
```

(this will of course require leiningen to be installed)

## Usage

See: [Docs](docs/index.org "Docs") for user guide.

When it exists, you would run that command below.

    $ java -jar ahubu-0.1.0-standalone.jar [args]

## Options

### Custom keybinds/behavior

Ahubu looks for configuration in `$HOME/.ahuburc` and `$XDG_CONFIG_HOME/ahubu/ahuburc`,
which overrides the defaults. To get started customizing, copy the default from
`conf/default-rc` in this project to either of those locations and modify accordingly.

You can bind any of the following to your key presses:

- A valid Clojure function call (or expression, such as a Clojure lambda)
- A string literal (this will run as javascript in the browser)
- A keyword bind can be a keyword letter, OR a string (useful for CTRL + key binds)

At the moment, rebinding :keymaps will require you to fully spec out your custom keymaps,
so make sure to start with a full config and customize as needed.

```clojure

;; -*- mode: clojure -*-

{
 ;; These shadow into the keybinds with a more concise syntax.
 :quickmarks
 {
  :d "http://ddg.gg"
  }

 ;; These tie to the various modes.
 :keymaps
 {
  :default
  {
   :z "my_custom_js_call()"
   :y ahubu.lib/any-builtin-function
   :x #(println "Hello)
  }
 }
```

### Network Filtering

Look in conf/url-ignore-regexes.txt for now - any URL patterns you add
here (newline separated) will be stopped from downloading at the
browser's network request level (aka, your browser will never even
attempt to download those requests - effectively blackholing garbage
you don't care about receiving).

### Hooks

Coming soon

## License

Copyright © 2018 Matthew Carter <m@ahungry.com>

Distributed under the GNU General Public License either version 3.0 or (at
your option) any later version.
