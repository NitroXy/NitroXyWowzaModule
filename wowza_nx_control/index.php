<?php require('config.php'); ?>
<?php

function url_dash($stream){
	global $config;
	return "http://{$config['rtmp_host']}:1935/nitroxy/{$stream}/manifest.mpd";
}

function url_hls($stream){
	global $config;
	return "http://{$config['rtmp_host']}:1935/nitroxy/{$stream}/playlist.m3u8";
}

function url_rtsp($stream){
	global $config;
	return "rtsp://{$config['rtmp_host']}:1935/nitroxy/{$stream}";
}

?>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<title>NitroXy Media Control</title>
		<link href="bootstrap.min.css" rel="stylesheet">
		<link href="bootstrap-switch.min.css" rel="stylesheet">
		<link rel="stylesheet" href="//releases.flowplayer.org/6.0.3/skin/functional.css">
		<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css">
		<link href="style.css" rel="stylesheet">
		<script src="jquery.min.js"></script>
		<script type='text/javascript' src="nitroxy.js"></script>
	</head>
	<body>
		<div class="container-fluid" style="margin: 15px">
			<div id="disconnected" style="display: none;">
				<p>Lost connection to backend, reload page to retry.</p>
				<i class="fa fa-2x fa-chain-broken"></i>
			</div>
			<h1>NitroXy media control</h1>

			<div id="messages"></div>

			<div class="row vertical-align">
				<div class="col-sm-5 col-sm-push-6 live">
					<div class="inner">
						<h2>Live <span class="published fa fa-circle"></span></h2>
						<div class="flowplayer" data-live="true">
							<video data-title="Live stream">
								<source type="application/x-mpegurl" src="<?=url_hls($config['live_stream'])?>">
							</video>
						</div>
						<div id="live_data"></div>
					</div>
				</div>

				<div class="col-sm-1 actions">
					<div class="inner">
						<button type="button" id="publish" class="btn btn-success btn-lg" title="Publish previewed stream to live stream">
							<span class="fa publish-icon">
						</button>
					</div>
				</div>

				<div class="col-sm-5 col-sm-pull-6  preview">
					<div class="inner">
						<h2>Preview</h2>
						<div class="flowplayer" data-live="true">
							<video data-title="Preview stream">
								<source type="application/x-mpegurl" src="<?=url_hls($config['preview_stream'])?>">
							</video>
						</div>
						<div id="preview_data"></div>
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-md-5">
					<h4>Change preview stream</h4>
					<p>Use this to preview video before pushing changes to live stream. Due to inherited buffering changes will not be visible immediately so be patient.</p>

					<form class="form-group" id="preview-stream-list">
						<label class="control-label">Available streams</label>
						<div class="input-group">
							<span class="input-group-btn">
								<button class="btn btn-default" data-action="refresh-streams" type="button" title="Refresh stream list"><span class="fa fa-refresh"></span></button>
							</span>
							<select id="preview_stream_list" class="form-control stream-list"></select>
							<span class="input-group-btn">
								<button class="btn btn-primary" type="submit" title="Change to selected"><span class="fa fa-play"></span></button>
							</span>
						</div>
					</form>

					<form class="form-group" id="preview-stream-manual">
						<label class="control-label">Select manually</label>
						<div class="input-group">
							<input type="text" id="preview_stream" class="form-control" placeholder="Stream name"/>
							<span class="input-group-btn">
								<button class="btn btn-default" type="submit" title="Change to selected"><span class="fa fa-play"></span></button>
							</span>
						</div>
					</form>
				</div>
				<div class="col-md-1 hidden-xs hidden-sm"></div>
				<div class="col-md-5">
					<h4>Actions</h4>
					<div class="panel panel-default live-control">
						<div class="panel-heading">
							<a class="btn-block collapsed" role="button" data-toggle="collapse" href="#live-controls" aria-expanded="false" aria-controls="live-controls">
								Live stream controls.
								<span class="fa pull-right"></span>
							</a>
						</div>
						<div class="panel-body collapse" id="live-controls">
							<p>These actions controls the live stream, be careful!</p>
							<button type="button" id="republish" class="btn btn-warning">
								<span class="fa fa-fw fa-cogs"></span> Restart broadcast
							</button>
							<button type="button" id="stop-stream" class="btn btn-danger">
								<span class="fa fa-fw fa-stop"></span> Stop broadcast
							</button>

							<h4>External publishing (Twitch)</h4>
							<input type="checkbox" data-switch id="external-publish" />
						</div>
					</div>

					<h4>Segmentation</h4>
					<p>Split ongoing recording into a new file. Preferably this should be done before each match or some other activity.</p>

					<form class="form-group">
						<button type="button" id="segment-current" class="btn btn-primary" title="Segment the stream which is currently live"><span class="fa fa-eject"></span> Segment live</button>
					</form>

					<form class="form-group" id="segment-stream">
						<label class="control-label">Segment stream</label>
						<div class="input-group">
							<span class="input-group-btn">
								<button class="btn btn-default" type="button" data-action="refresh-streams" title="Refresh stream list"><span class="fa fa-refresh"></span></button>
							</span>
							<select class="form-control stream-list" data-filter="stream"></select>
							<span class="input-group-btn">
								<button class="btn btn-default" type="submit" title="Segment selected"><span class="fa fa-eject"></span></button>
							</span>
						</div>
					</form>
				</div>
			</div>

			<div class="row">
				<div class="col-md-5">
					<h4>Fallback stream</h4>
					<p>The fallback stream is played when the regular stream is finished or down.</p>

					<form class="form-group fallback-stream" id="fallback-stream">
						<label class="control-label">Available streams</label>
						<div class="input-group">
							<span class="input-group-btn">
								<button class="btn btn-default" type="button" data-action="refresh-streams" title="Refresh stream list"><span class="fa fa-refresh"></span></button>
							</span>
							<select class="form-control stream-list"></select>
							<span class="input-group-btn">
								<button class="btn btn-default" type="submit" title="Change to selected"><span class="fa fa-play-circle"></span></button>
							</span>
						</div>
						<span class="help-block current"></span>
					</form>
				</div>
			</div>
		</div>

		<footer class="container-fluid">
			<h2>External player</h2>
			<div class="row external">
				<div class="col-md-5">
					<h4>Preview</h4>
					<dl class="dl-horizontal">
						<dt>DASH</dt><dd><a href="<?=url_dash($config['preview_stream']);?>"><?=url_dash($config['preview_stream']);?></a></dd>
						<dt>HLS</dt><dd><a href="<?=url_hls($config['preview_stream']);?>"><?=url_hls($config['preview_stream']);?></a></dd>
						<dt>RTSP</dt><dd><a href="<?=url_rtsp($config['preview_stream']);?>"><?=url_rtsp($config['preview_stream']);?></a></dd>
					</dl>
				</div>
				<div class="col-md-1"></div>
				<div class="col-md-5">
					<h4>Live</h4>
					<dl class="dl-horizontal">
						<dt>DASH</dt><dd><a href="<?=url_dash($config['live_stream']);?>"><?=url_dash($config['live_stream']);?></a></dd>
						<dt>HLS</dt><dd><a href="<?=url_hls($config['live_stream']);?>"><?=url_hls($config['live_stream']);?></a></dd>
						<dt>RTSP</dt><dd><a href="<?=url_rtsp($config['live_stream']);?>"><?=url_rtsp($config['live_stream']);?></a></dd>
					</dl>
				</div>
			</div>
		</footer>

		<script defer src="//releases.flowplayer.org/6.0.3/flowplayer.min.js"></script>
		<script src="bootstrap.min.js"></script>
		<script src="bootstrap-switch.min.js"></script>
	</body>
</html>
