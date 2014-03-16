<?php

$cmd_host = "localhost";
$cmd_port = 1337;

$data = $_POST['json'];

if(isset($data)) {
		$sck=socket_create(AF_INET,SOCK_STREAM,0) or die("Could not create socket");
		if(socket_connect($sck, $cmd_host, $cmd_port)) {
			socket_write($sck, $data."\n");
			$res = socket_read($sck, 1024, PHP_NORMAL_READ);
			echo $res;
		}
		socket_close($sck);
}
