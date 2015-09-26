var error_count = 0;
var update_timer;
var error_timer;

/**
 * Make async RPC call to the backend control. It returns a jQuery Deferred
 * object (quite similar to $.ajax)
 */
function remoteCall(function_name /* args ...*/) {
	var dfd = $.Deferred();
	var args = jQuery.makeArray(arguments).slice(1)

	try {
		$.ajax({
			url: 'backend.php',
			type: 'POST',
			contentType: 'application/json',
			processData: false,
			data: JSON.stringify({
				'function': function_name,
				'args': args,
			}),
		}).done(function(reply){
			if ( reply.status === 'success' ){
				dfd.resolve(reply.data);
			} else {
				console.error("Remote error: ", reply)
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
 * Makes a simple RPC call where first argument is always the function name and
 * the rest is passed as regular arguments. On success the stream info is
 * updated and on errors a generic error message is shown.
 */
function simpleCall(func /* args ... */){
	remoteCall.apply(this, arguments).done(function(){
		updateStreamInfo();
	}).fail(function(){
		alert("Warning, SwitchStream is not enabled on the server");
	});
}

function wrapCall(func){
	var args = arguments;
	return function(){
		return remoteCall.apply(this, args);
	};
}

/* "low"-level API access, this is the raw functions that is available on the server */
var api = {};
api.switchStream = wrapCall('switchStream');
api.publishStream = wrapCall('publishStream');
api.getStreams = wrapCall('getStreams');
api.fullStatus = wrapCall('fullStatus');
api.currentLive = wrapCall('currentLive');
api.currentPreview = wrapCall('currentPreview');
api.setFallback = wrapCall('setFallback');
api.currentFallback = wrapCall('currentFallback');
api.restartBroadcast = wrapCall('restartBroadcast');
api.stopPushPublish = wrapCall('stopPushPublish');
api.startPushPublish = wrapCall('startPushPublish');
api.segment = wrapCall('segment');

$(function() {
	updateStreamInfo();
	updateStreamList();

	update_timer = setInterval(function(){
		if ( !isUpdating ){
			updateStreamInfo();
		} else {
			console.error('Update skipped because the previous request is still ongoing.');
		}
	}, 10000);

	error_timer = setInterval(function(){
		if ( error_count < 5 ){
			error_count = 0;
		} else {
			console.error('Too many errors, bailing out');
			$('#disconnected').show();
			clearInterval(update_timer);
			clearInterval(error_timer);
		}
	}, 60 * 1000);

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

	$("#publish").click(function(e) {
		e.preventDefault();
		publish();
	})

	$("#republish").click(function(e) {
		e.preventDefault();
		if(confirm("Are you sure you want to restart live broadcasting?")) {
			restartBroadcast();
		}
	})

	$("#stop-stream").click(function(e) {
		e.preventDefault();
		if(confirm("Are you sure you want to stop live broadcasting?")) {
			stop();
		}
	})

	$('button[data-action="refresh-streams"]').click(function(e) {
		e.preventDefault();
		updateStreamList();
	})

	$("form#fallback-stream").submit(function(e) {
		e.preventDefault();
		setFallbackStream($("#fallback-stream select").val());
	})

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
})

var isUpdating = false;
function updateStreamInfo() {
	isUpdating = true;
	api.fullStatus().done(function(data){
		$("#live_data").html("Current stream: " + data.live_target);
		$("#preview_data").html("Current stream: " + data.preview_target);
		$("#fallback-stream .current").html("Current fallback: " + data.fallback_target);
		$('.published').toggle(data.is_published);
	}).always(function(){
		isUpdating = false;
	});
}

function switchStream(stream) {
	if(!stream || !$.trim(stream)) {
		alert("Can't change to empty stream");
		return false;
	}
	simpleCall("switchStream", stream);
}

function publish() {
	simpleCall("publishStream");
}

function restartBroadcast() {
	simpleCall("restartBroadcast");
}

function stop() {
	simpleCall("stopPushPublish");
}

function setFallbackStream(stream) {
	if(!stream || !$.trim(stream)) {
		alert("Can't change to empty stream");
		return false;
	}
	simpleCall("setFallback", stream);
}

function updateStreamList() {
	var form = $('.stream-list').parents('form');
	form.addClass('loading');
	form.removeClass('has-error');
	form.find('.help-block.error').remove();

	api.getStreams().done(function(streams){
		var list = $('.stream-list');
		list.empty();

		/* sort so actual streams comes first and VOD after */
		streams.sort(function(a,b){
			var ac = a.indexOf(':') >= 0;
			var bc = b.indexOf(':') >= 0;
			if ( (ac && bc) || !(ac || bc) ){
				return a.localeCompare(b);
			} else if ( ac && !bc ){
				return 1;
			} else if ( !ac && bc ){
				return -1;
			}
		});

		/* fill each list individually (can have different filters) */
		list.each(function(){
			var target = $(this);
			var filter = target.data('filter');

			/* first is always blank empty */
			target.append('<option></option>');

			$.each(streams, function(idx, stream) {
				switch ( filter ){
				case 'stream':
					if ( stream.indexOf(':') >= 0 ) return;
				default:
					break;
				}

				target.append("<option>"+stream+"</option>")
			});
		});
	}).fail(function(){
		form.addClass('has-error');
		form.append('<span class="help-block error">Failed to update stream list, see console log for defaults.</span>');
	}).always(function(){
		form.removeClass('loading');
	});
}
