<?php require('config.php'); ?>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<title>NitroXy Media Control</title>
		<link href="bootstrap.min.css" rel="stylesheet">
		<link rel="stylesheet" href="//releases.flowplayer.org/6.0.3/skin/functional.css">
		<link href="style.css" rel="stylesheet">
		<script src="jquery.min.js"></script>
		<script type='text/javascript' src="nitroxy.js"></script>
	</head>
	<body>
		<div class="container-fluid" style="margin: 15px">
			<h1>NitroXy media control</h1>

			<div class="row">
				<div class="col-md-5">
					<h2>Preview</h2>
					<div class="flowplayer" data-live="true">
						<video data-title="Live stream">
							<source type="application/x-mpegurl" src="http://<?=$config['rtmp_host']?>:1935/nitroxy/<?=$config['preview_stream']?>/playlist.m3u8">
						</video>
					</div>
					<div id="preview_data"></div>
					<div>
						<h4>Change preview stream</h4>

						<form class='form-inline' style='margin-bottom: 10px'>
							<select id='preview_stream_list' class='form-control' style='width: 150px'></select>
							<input type='submit' value='Change to selected' id='do_change_from_list' class='btn btn-primary'/>
						</form>

						<form class='form-inline' style='margin-bottom: 10px'>
							<input type='text' id='preview_stream' class='form-control' style='width: 150px'/>
							<input type='submit' value='Change' id='do_change' class='btn btn-primary'/>
						</form>

						<form class='form-inline' style='margin-bottom: 10px'>
							<input type='submit' value='Refresh stream list' id='refresh_streams' class='btn btn-default'/>
						</form>
					</div>
				</div>

				<div class="col-md-5">
					<h2>Live <span class="published">&#x2b24;</span></h2>
					<div class="flowplayer" data-live="true">
						<video data-title="Live stream">
							<source type="application/x-mpegurl" src="http://<?=$config['rtmp_host']?>:1935/nitroxy/<?=$config['live_stream']?>/playlist.m3u8">
						</video>
					</div>
					<div id="live_data"></div>
					<div>
						<form class='form-inline' style='margin-bottom: 10px'>
							<input type='submit' value='Publish' id='publish' class='btn btn-success'/>
						</form>

						<form class='form-inline' style='margin-bottom: 10px'>
							<input type='submit' value='Republish' id='republish' class='btn btn-warning'/>
						</form>
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-md-5">
					<h4>Fallback stream</h4>
					<div id="fallback_stream"></div>
					<form class='form-inline' style='margin-bottom: 10px'>
						<select id='fallback_stream_list' class='form-control' style='width: 150px'></select>
						<input type='submit' value='Change to selected' id='do_fallback_change' class='btn btn-primary'/>
					</form>
				</div>
			</div>
		</div>

		<script defer src="//releases.flowplayer.org/6.0.3/flowplayer.min.js"></script>
	</body>
</html>
