<?php

$cmd_host = "localhost";
$cmd_port = 1337;
$chunk_size = 4096;

$data = $_POST['json'];

header('Content-Type: application/json; charset=UTF-8');

if(isset($data)) {
	$sck=socket_create(AF_INET,SOCK_STREAM,0) or die("Could not create socket");
	if(socket_connect($sck, $cmd_host, $cmd_port)) {
		socket_write($sck, $data."\n");

		/* first chunk is read blocking in order to wait for the server to process the command*/
		$res = socket_read($sck, $chunk_size, PHP_BINARY_READ);
		echo $res;

		/* read the rest of the chunks in non-blocking mode until there is no more data */
		socket_set_nonblock($sck);
		while ( $res = socket_read($sck, $chunk_size, PHP_BINARY_READ) ){
			echo $res;
		}
	}
	socket_close($sck);
}
