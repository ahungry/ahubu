<?php

//print_r($_COOKIES);
print_r($_COOKIE);

$x = 1 + ($_COOKIE['bar'] ?? 0);

setcookie('bar', $x, strtotime('+1 year'));

echo 'hi: ' . $x;
