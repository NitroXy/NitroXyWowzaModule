<?php

$config = [
	/**
	 * IP or hostname to the RTMP stream.
	 * Default assumes RTMP is running on the same machine.
	 */
	'rtmp_host' => $_SERVER['HTTP_HOST'],

	/**
	 * Stream names.
	*/
	'live_stream' => 'nitroxy',
	'preview_stream' => 'preview',
];
