# AHUBU (ahungry-browser)

![ahubu](https://github.com/ahungry/ahubu/blob/master/ahubu.png)

## AHUBU Helps Users Be Utilitarian

A doctrine that the useful is the good and that the determining
consideration of right conduct should be the usefulness of its
consequences; specifically : a theory that the aim of action should be
the largest possible balance of pleasure over pain or the greatest
happiness of the greatest number .

> Stinson notes that many computer scientists have an implicit
> orientation to utilitarianism, an ethical theory that aims to maximize
> happiness for the greatest number by adding up each action’s costs and
> benefits.
> —
> molly driscoll, The Christian Science Monitor, "‘2001: A Space
> Odyssey’ turns 50: Why HAL endures," 3 Apr. 2018

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

## Todones and Todos

Built in regex based URL filtering (dynamic content
filtering/replacement to come).

Custom interaction between the website and the ahubu is
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

Make sure you have a java distribution with openjfx available (on Arch
Linux openjdk + openjfx worked out of the box.  On Ubuntu 18.04 I
needed to do some extra work).

### Java Dependencies

#### Arch Linux

Just install openjdk and openjfx

#### Ubuntu 18.04

Install this:

```sh
sudo add-apt-repository ppa:linuxuprising/java
sudo apt update
sudo apt install oracle-java10-installer # accept the license
sudo apt install oracle-java10-set-default
sudo apt install libcanberra-gtk3-dev libcanberra-gtk-module
```

Run the following (until I am distributing the uberjar to run a standalone):

```
lein deps
lein run -m ahubu.core
```

(this will of course require leiningen to be installed)

## Usage

When it exists, you would run that command below.

    $ java -jar ahubu-0.1.0-standalone.jar [args]

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
