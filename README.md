# ahungry-browser

A very customizable browser - initial keybindings will be a mesh
between Emacs and VIM, as I use Emacs with Evil bindings and that's my
preference.

## Todones and Todos

Built in regex based URL filtering (dynamic content
filtering/replacement to come).

Custom interaction between the website and the ahungry-browser is
possible thanks to JavaFX allowing 2 way calling (which means, in
theory, I can set up a per-page Lisp REPL for the user to go way
beyond standard page scraping integration some other browser add ons
may do - we could have a bind on each email in a gmail inbox, have the
page receive dynamically injected javasript, and 'call home' to this
browser - posting the email subjects to this browser to relay to Emacs
or whatever other endpoint we wish).

## WARNING

USE AT YOUR OWN RISK.  Make NO assumption of security or protection
from this naive browser implementation.  It is built on the webkit
that is bundled with JavaFX (tested only with openjfx in my case).

I'm noodling some ideas for a superior way to standbox a browser such
as this, and I think it might actually be a good idea to run such a
browser in an Alpine Docker image - that would mean your browser is
100% isolated and sandboxed from your host OS (we could allow the
virtual mount for sharing data between the sandbox and some select
host OS drive as well).

## Installation

Run the following (until I am distributing the uberjar to run a standalone):

```
lein deps
lein run -m ahungry-browser.core
```

(this will of course require leiningen to be installed)

## Usage

When it exists, you would run that command below.

    $ java -jar ahungry-browser-0.1.0-standalone.jar [args]

## Options

Look in conf/url-ignore-regexes.txt for now - any URL patterns you add
here (newline separated) will be stopped from downloading at the
browser's network request level (aka, your browser will never even
attempt to download those requests - effectively blackholing garbage
you don't care about receiving).

## License

Copyright © 2018 Matthew Carter <m@ahungry.com>

Distributed under the GNU General Public License either version 3.0 or (at
your option) any later version.
