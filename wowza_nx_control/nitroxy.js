/**
 * Make async RPC call to the backend control. It returns a jQuery Deferred
 * object (quite similar to $.ajax)
 */
function remoteCall(function_name /* args ...*/) {
	var dfd = $.Deferred();
	var args = jQuery.makeArray(arguments).slice(1)

	try {
		$.post('backend.php', {
			json: JSON.stringify({
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
			console.error("Remote error: ", reply)
			dfd.reject(reply);
		});
	} catch (e){
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

$(function() {
	updateStreamInfo();
	updateStreamList();

	setInterval(updateStreamInfo, 10000);

	$('form.preview-stream').submit(function(e){
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
			republish();
		}
	})

	$("#stop-stream").click(function(e) {
		e.preventDefault();
		if(confirm("Are you sure you want to stop live broadcasting?")) {
			stop();
		}
	})

	$("#refresh-streams").click(function(e) {
		e.preventDefault();
		updateStreamList();
	})

	$("#do_fallback_change").click(function(e) {
		e.preventDefault();
		setFallbackStream($("#fallback_stream_list").val());
	})

	$('#preview-stream-manual .form-control').on('change keyup paste', function(){
		var group = $(this).parents('.form-group');
		var submit = group.find('button');
		var empty = $(this).val().length === 0;
		submit.prop('disabled', empty);
	}).change();
})

function updateStreamInfo() {
	$.post('backend.php', {
		json: JSON.stringify({
			'function': 'fullStatus',
			'args': []
		}),
	}).done(function(reply){
		if ( reply.status === 'success' ){
			$("#live_data").html("Current stream: " + reply.data.live_target);
			$("#preview_data").html("Current stream: " + reply.data.preview_target);
			$("#fallback_stream").html("Current fallback: " + reply.data.fallback_target);
			$('.published').toggle(reply.data.is_published !== 'no');
		} else {
			console.error("Remote error: ", reply)
		}
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

function republish() {
	simpleCall("republish");
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
	var form = $('#preview-stream-list');
	form.addClass('loading');

	remoteCall('getStreams').done(function(streams){
		var list = $('#preview-stream-list select, #fallback_stream_list');
		list.empty();
		streams.sort();
		$.each(streams, function(idx, stream) {
			list.append("<option>"+stream+"</option>")
		});
	}).fail(function(){
		alert('Failed to update stream list, see console log for details');
	}).always(function(){
		form.removeClass('loading');
	});
}
