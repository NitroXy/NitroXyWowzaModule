(function(){
	'use strict';

	var switch_hack = false;                /* hack to prevent programmatic updates from triggering change callback */
	var error_count = 0;
	var update_timer;
	var error_timer;
	var update_interval = 10 * 1000;
	var error_interval = 60 * 1000;
	var segment_timer_element;

	/**
	 * Make async RPC call to the backend control. It returns a jQuery Deferred
	 * object (quite similar to $.ajax)
	 */
	function remoteCall(function_name, method, args){
		var dfd = $.Deferred();
		var url = '/api/' + function_name;

		try {
			$.ajax({
				url: url,
				type: method,
				contentType: 'application/json',
				processData: false,
				data: typeof(args) !== 'undefined' ? JSON.stringify(args) : undefined,
			}).done(function(reply){
				if ( reply.status === 'success' ){
					dfd.resolve(reply.data);
				} else {
					console.error("Remote error: ", reply);
					dfd.reject(reply);
				}
			}).error(function(reply){
				error_count++;
				console.error("Remote error: ", reply.responseJSON || reply.responseText);
				dfd.reject({
					status: 'error',
					error: url + ' - ' + reply.statusText,
				});
			});
		} catch (e){
			error_count++;
			console.error(e);
			dfd.reject(e);
		}

		return dfd.promise();
	}

	/**
	 * Create a function wrapped around a remoteCall.
	 * E.g. wrapCall('foo') returns a function calling remoteCall('foo', args..)
	 */
	function wrapCall(func, options){
		var defaults = {
			update: true,
			error: true,
			method: 'POST',
		};
		var o = $.extend({}, defaults, options || {});

		return function(args){
			var dfd = remoteCall.call(this, func, o.method, args);

			if ( o.update ){
				dfd.done(function(){
					updateStreamInfo();
				});
			}

			if ( o.error ){
				dfd.fail(function(data){
					console.log(data);
					$('#messages').append(
						'<div class="alert alert-danger alert-dismissable role="alert">' +
							'<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
							'Remote call failed: ' + data.error + '<br/>' +
							(typeof(data.stracktrace) != 'undefined' ?
							 'Callstack:<br/><pre>' +
							 JSON.stringify(data.stacktrace, null, 2)
							 : '') +
							'</pre>' +
						'</div>'
					);
				});
			}

			return dfd;
		};
	}

	/* "low"-level API access, this is the raw functions that is available on the server */
	var api = {};
	api.switchStream = wrapCall('stream/switch', {update: true, error: true});
	api.publishStream = wrapCall('stream/push', {update: true, error: true});
	api.publishExternal = wrapCall('publish', {update: true, error: true});
	api.getStreams = wrapCall('streams', {method: 'GET', update: false, error: true});
	api.fullStatus = wrapCall('status', {method: 'GET', update: false, error: false});
	api.setFallback = wrapCall('stream/fallback', {update: true, error: true});
	api.restartBroadcast = wrapCall('stream/restart', {update: true, error: true});
	api.stopBroadcast = wrapCall('stream/stop', {update: true, error: true});
	api.recording = {
		toggle:  wrapCall('recording', {update: true, error: true}),
		segment: wrapCall('recording/segment', {update: false, error: true}),
	};


	function zeropad(s, n){
		s = '' + s;
		while ( s.length < n ){
			s = '0' + s;
		}
		return s;
	}

	function hhmmss(seconds){
		var h = Math.floor(seconds / 3600);
		var m = Math.floor(seconds / 60) % 60;
		return zeropad(h, 2) + ':' + zeropad(m, 2);
	}

	function update_segment_duration(duration){
    if ( typeof(duration) === 'undefined' ) return;
		if ( typeof(segment_timer_element) === 'undefined' || segment_timer_element.length === 0 ) return;
		segment_timer_element.html(hhmmss(duration));
	}

	var isUpdating = false;
	function updateStreamInfo(){
		isUpdating = true;
		api.fullStatus().done(function(data){
			switch_hack = true; /* prevent change callbacks from firing */
			try {
				$("#live_data").html("Current stream: " + data.live_target);
				$("#preview_data").html("Current stream: " + data.preview_target);
				$("#fallback-stream .current").html("Current fallback: " + data.fallback_target);
				$('.published').toggle(data.is_published);
				$('input#external-publish').bootstrapSwitch('state', data.is_published);
				$('input#auto-recording').bootstrapSwitch('state', data.auto_recording);
			  update_segment_duration(data.segment ? data.segment.duration : undefined);
			} finally {
				switch_hack = false;
			}

			$('body').toggleClass('stream-recording', data.auto_recording);
		}).always(function(){
			isUpdating = false;
		});
	}

	function switchStream(stream){
		if(!stream || !$.trim(stream)){
			alert("Can't change to empty stream");
			return;
		}
		api.switchStream({name: stream});
	}

	function setFallbackStream(stream){
		if(!stream || !$.trim(stream)){
			alert("Can't change to empty stream");
			return;
		}
		api.setFallback({name: stream});
	}

	function updateStreamList(){
		var form = $('.stream-list').parents('form');
		form.addClass('loading');
		form.removeClass('has-error');
		form.find('.help-block.error').remove();

		api.getStreams().done(function(streams){
			var list = $('.stream-list');
			list.empty();

			/* sort so actual streams comes first and VOD after */
			streams.sort(function(a, b){
				var ac = a.indexOf(':') >= 0;
				var bc = b.indexOf(':') >= 0;
				if ( (ac && bc) || !(ac || bc) ){
					return a.localeCompare(b);
				} else if ( ac && !bc ){
					return 1;
				} else if ( !ac && bc ){
					return -1;
				} else {
					return 0; /* linter SA isn't cleaver enough to figure out this is never reached */
				}
			});

			/* fill each list individually (can have different filters) */
			list.each(function(){
				var target = $(this);
				var filter = target.data('filter');

				/* first is always blank empty */
				target.append('<option></option>');

				$.each(streams, function(idx, stream){
					switch ( filter ){
					case 'stream':
						if ( stream.indexOf(':') >= 0 ) return;
						break;
					default:
						break;
					}

					target.append("<option>"+stream+"</option>");
				});
			});
		}).fail(function(){
			form.addClass('has-error');
			form.append('<span class="help-block error">Failed to update stream list, see console log for defaults.</span>');
		}).always(function(){
			form.removeClass('loading');
		});
	}

	function template(str, data){
		return str.replace(/{([a-z_]+)}/g, function(_, key){
			return data[key];
		});
	}

	function url_dash(stream){
		return template("http://{rtmp_host}:1935/nitroxy/{stream}/manifest.mpd", {
			rtmp_host: window.location.hostname,
			stream: stream,
		});
	}

	function url_hls(stream){
		return template("http://{rtmp_host}:1935/nitroxy/{stream}/playlist.m3u8", {
			rtmp_host: window.location.hostname,
			stream: stream,
		});
	}

	function url_rtsp(stream){
		return template("rtsp://{rtmp_host}:1935/nitroxy/{stream}", {
			rtmp_host: window.location.hostname,
			stream: stream,
		});
	}

	$(function(){
		updateStreamInfo();
		updateStreamList();

		update_timer = setInterval(function(){
			if ( !isUpdating ){
				updateStreamInfo();
			} else {
				console.error('Update skipped because the previous request is still ongoing.');
			}
		}, update_interval);

		error_timer = setInterval(function(){
			if ( error_count < 5 ){
				error_count = 0;
			} else {
				console.error('Too many errors, bailing out');
				$('#disconnected').show();
				clearInterval(update_timer);
				clearInterval(error_timer);
			}
		}, error_interval);

		segment_timer_element = $('#segment-timer');

		$('form#preview-stream-list').submit(function(e){
			e.preventDefault();
			var control = $(this).find('.form-control');
			var stream = control.val();
			switchStream(stream);

			/* reset name for manual control */
			if ( control.attr('type') === 'text' ){
				control.val('');
			}
		});

		$("#publish").click(function(e){
			e.preventDefault();
			api.publishStream();
		});

		$("#republish").click(function(e){
			e.preventDefault();
			if(confirm("Are you sure you want to restart live broadcasting?")){
				api.restartBroadcast();
			}
		});

		$("#stop-stream").click(function(e){
			e.preventDefault();
			if(confirm("Are you sure you want to stop live broadcasting?")){
				api.stopBroadcast();
			}
		});

		$('button[data-action="refresh-streams"]').click(function(e){
			e.preventDefault();
			updateStreamList();
		});

		$("form#fallback-stream").submit(function(e){
			e.preventDefault();
			setFallbackStream($("#fallback-stream select").val());
		});

		$('#preview-stream-manual .form-control').on('change keyup paste', function(){
			var group = $(this).parents('.form-group');
			var submit = group.find('button');
			var empty = $(this).val().length === 0;
			submit.prop('disabled', empty);
		}).change();

		$('#segment-current').click(function(e){
			e.preventDefault();
			api.recording.segment();
		});

		$('form#segment-stream').submit(function(e){
			e.preventDefault();
			var select = $(this).find('select');
			var stream = select.val();
			if ( stream ){
				api.recording.segment({stream: stream});
			}
			select.val('');
		});

		$('input#external-publish').change(function(){
			if ( switch_hack ) return;
			var on = $(this).prop('checked');
			api.publishExternal({state: on});
		});

		$('input#auto-recording').change(function(){
			if ( switch_hack ) return;
			var on = $(this).prop('checked');
			api.recording.toggle({state: on});
		});

		$('input[data-switch]').bootstrapSwitch().on('switchChange.bootstrapSwitch', function(e, state){
			$(this).prop('checked', state).change();
		});

		var data_url = {
			'dash': url_dash,
			'hls': url_hls,
			'rtsp': url_rtsp,
		};

		for ( var key in data_url ){
			var url = data_url[key];
			$('[data-' + key + '-src]').each(function(){
				var stream = $(this).data(key + '-src');
				$(this).attr('src', url(stream));
			});
			$('[data-' + key + '-href]').each(function(){
				var stream = $(this).data(key + '-href');
				$(this).attr('href', url(stream));
			});
			$('[data-' + key + '-text]').each(function(){
				var stream = $(this).data(key + '-text');
				$(this).text(url(stream));
			});
		}

		if ( typeof(window.flowplayer) == 'undefined' ){
			console.error('flowplayer.js was not loaded (check permissions, e.g. noscript)');
		}

		$(".player").flowplayer();
	});
})();
