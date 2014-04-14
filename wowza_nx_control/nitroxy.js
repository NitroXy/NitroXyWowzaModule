var server = "rtmp://localhost/live"
var previewStream = "preview";
var liveStream = "nitroxy";

function remoteCall(function_name /* args ...*/) {
	var args = jQuery.makeArray(arguments).slice(1)

	var sjax = new XMLHttpRequest();
	sjax.open("POST", "backend.php", false);
	sjax.setRequestHeader("Content-type","application/x-www-form-urlencoded");
	sjax.send("json="+JSON.stringify({'function': function_name, 'args': args}));

	try {
		var ret = JSON.parse(sjax.responseText);
		if(ret.status == "success") {
			return ret.data
		} else {
			console.log("Remote error: " + ret["data"])
			return false;
		}
	} catch (err) {
		console.log("remoteCall error, is the remote command up (and is the application started?) ("+err+")");
		return false;
	}
}

$(function() {
	updateStreamInfo();
	updateStreamList();

	$("#do_change").click(function() {
		switchStream($("#preview_stream").val());
		return false;
	})

	$("#do_change_from_list").click(function() {
		switchStream($("#preview_stream_list").val());
		return false;
	})

	$("#publish").click(function() {
		publish();
		return false;
	})

	$("#republish").click(function() {
		if(confirm("Are you sure you want to republish?")) {
			republish();
		}
		return false;
	})

	$("#refresh_streams").click(function() {
		updateStreamList();
		return false;
	})

	$("#do_fallback_change").click(function() {
		setFallbackStream($("#fallback_stream_list").val());
		return false;
	})
})

function updateStreamInfo() {
	$("#live_data").html("Current stream: " + remoteCall("currentLive"));
	$("#preview_data").html("Current stream: " + remoteCall("currentPreview"));
	$("#fallback_stream").html("Current fallback: " + remoteCall("currentFallback"));
}

function switchStream(stream) {
	if(!stream || !$.trim(stream)) {
		alert("Can't change to empty stream");
		return false;
	}
	if(!remoteCall("switchStream", stream)) {
		alert("Warning, SwitchStream is not enabled on the server");
	}
	updateStreamInfo();
}

function publish() {
	if(!remoteCall("publishStream")) {
		alert("Warning, SwitchStream is not enabled on the server");
	}
	updateStreamInfo();
}

function republish() {
	if(!remoteCall("republish")) {
		alert("Warning, SwitchStream is not enabled on the server");
	}
	updateStreamInfo();
}

function setFallbackStream(stream) {
	if(!stream || !$.trim(stream)) {
		alert("Can't change to empty stream");
		return false;
	}
	remoteCall("setFallback", stream);
	updateStreamInfo();
}

function updateStreamList() {
	var streams = remoteCall('getStreams');

	var $list = $("#preview_stream_list")
	var $list2 = $("#fallback_stream_list")

	$list.html("");
	$list2.html("");

	$.each(streams, function(idx, stream) {
		$list.append("<option>"+stream+"</option>")
		$list2.append("<option>"+stream+"</option>")
	})
}
