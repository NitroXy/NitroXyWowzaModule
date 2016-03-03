(function(){
	'use strict';

	var error_count = 0;
	var update_timer;
	var error_timer;
	var update_interval = 10 * 1000;
	var error_interval = 60 * 1000;

	/**
	 * Make async RPC call to the backend control. It returns a jQuery Deferred
	 * object (quite similar to $.ajax)
	 */
	function remoteCall(function_name, method, args){
		var dfd = $.Deferred();

		try {
			$.ajax({
				url: '/api/' + function_name,
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
				dfd.reject(reply);
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
					$('#messages').append(
						'<div class="alert alert-danger alert-dismissable role="alert">' +
							'<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
							'Remote call failed: ' + data.error + '<br/>' +
							'Callstack:<br/><pre>' +
							JSON.stringify(data.stacktrace, null, 2) +
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
	api.switchStream = wrapCall('stream', {update: true, error: true});
	api.publishStream = wrapCall('publishStream', {update: true, error: true});
	api.publishExternal = wrapCall('publishExternal', {update: true, error: true});
	api.getStreams = wrapCall('streams', {method: 'GET', update: false, error: true});
	api.fullStatus = wrapCall('status', {method: 'GET', update: false, error: false});
	api.setFallback = wrapCall('setFallback', {update: true, error: true});
	api.restartBroadcast = wrapCall('restartBroadcast', {update: true, error: true});
	api.stopBroadcast = wrapCall('stopBroadcast', {update: true, error: true});
	api.segment = wrapCall('segment', {update: false, error: true});

	var isUpdating = false;
	function updateStreamInfo(){
		isUpdating = true;
		api.fullStatus().done(function(data){
			$("#live_data").html("Current stream: " + data.live_target);
			$("#preview_data").html("Current stream: " + data.preview_target);
			$("#fallback-stream .current").html("Current fallback: " + data.fallback_target);
			$('.published').toggle(data.is_published);
			$('input#external-publish').bootstrapSwitch('state', data.is_published);
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
		api.setFallback(stream);
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
			api.segment();
		});

		$('form#segment-stream').submit(function(e){
			e.preventDefault();
			var select = $(this).find('select');
			var stream = select.val();
			if ( stream ){
				api.segment(stream);
			}
			select.val('');
		});

		$('input#external-publish').change(function(){
			var on = $(this).prop('checked');
			api.publishExternal(on);
		});

		$('input[data-switch]').bootstrapSwitch().on('switchChange.bootstrapSwitch', function(e, state){
			$(this).prop('checked', state).change();
		});
	});
})();
