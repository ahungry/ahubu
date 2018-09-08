<?php

$x = $_COOKIE['x'] ?? 0;
setcookie('x', ++$x, time() + 3600 * 24 * 365 * 10, '/');
echo 'hi: ' . $x;
