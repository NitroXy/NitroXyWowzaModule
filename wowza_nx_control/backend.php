<?php

$cmd_host = "localhost";
$cmd_port = 1337;
$chunk_size = 4096;
$method = $_SERVER['REQUEST_METHOD'];

function error($code, $title){
	header("HTTP/1.0 $code $title");
	header('Content-Type: application/json; charset=UTF-8');
	echo json_encode([
		'status' => 'http_error',
		'code' => $code,
		'description' => $title,
	]);
	exit;
}

if ( $method != 'POST' ){
	error(405, 'Method Not Allowed');
}

/* read POST body */
$data = file_get_contents('php://input');
if ( empty($data) ){
	error(400, 'Bad Request');
}

$sck = socket_create(AF_INET,SOCK_STREAM,0) or die("Could not create socket");
socket_set_option($sck, SOL_SOCKET, SO_SNDTIMEO, array('sec' => 5, 'usec' => 0));
socket_set_option($sck, SOL_SOCKET, SO_RCVTIMEO, array('sec' => 5, 'usec' => 0));

if ( @socket_connect($sck, $cmd_host, $cmd_port) ){
	header('Content-Type: application/json; charset=UTF-8');
	socket_write($sck, $data."\n");

	/* first chunk is read blocking in order to wait for the server to process the command*/
	$res = socket_read($sck, $chunk_size, PHP_BINARY_READ);
	echo $res;

	/* read the rest of the chunks in non-blocking mode until there is no more data */
	socket_set_nonblock($sck);
	while ( $res = socket_read($sck, $chunk_size, PHP_BINARY_READ) ){
		echo $res;
	}
} else {
	error(503, 'Service Unavailable');
}

socket_close($sck);
