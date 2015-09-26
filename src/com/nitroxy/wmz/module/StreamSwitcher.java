package com.nitroxy.wmz.module;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.pushpublish.protocol.rtmp.PushPublishRTMP;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify;
import com.wowza.wms.stream.publish.Stream;

public class StreamSwitcher {
	private Stream liveStream = null;
	private Stream previewStream = null;

	private PushPublishRTMP publisher = null;

	private NitroXyModule main;

	private String realPreviewStream = null;
	private String realLiveStream = null;

	private Config config;
	private IApplicationInstance appInstance;
	private IMediaStreamActionNotify streamListener = new StreamListener();

	public StreamSwitcher(NitroXyModule main, IApplicationInstance appInstance, Config config) {
		this.main = main;
		this.config = config;
		this.appInstance = appInstance;

		main.info("StreamSwitcher::constructor()");

		/* create streams */
		main.info("StreamSwitcher creating streams live=" + liveStreamName() + ", preview=" + previewStreamName());
		liveStream = Stream.createInstance(appInstance, liveStreamName());
		previewStream = Stream.createInstance(appInstance, previewStreamName());

		/* start by playing fallback stream and wait for onPublish callback to start actual playback */
		playStream(previewStream, fallbackTarget());
		playStream(liveStream, fallbackTarget());
		realPreviewStream = previewTarget();
		realLiveStream = liveTarget();
	}

	public void close(){
		main.info("StreamSwitcher::close()");
		stopPushPublish();
		liveStream.close();
		previewStream.close();
	}

	protected String liveStreamName(){
		return config.settings.StreamSwitcher_liveStream;
	}

	protected String liveTarget(){
		return config.settings.StreamSwitcher_liveTarget;
	}

	protected String previewStreamName(){
		return config.settings.StreamSwitcher_previewStream;
	}

	protected String previewTarget(){
		return config.settings.StreamSwitcher_previewTarget;
	}
	
	protected String fallbackTarget(){
		return config.settings.StreamSwitcher_fallbackStream;
	}

	public void startPushPublish() {
		try {
			publisher = new PushPublishRTMP();
			publisher.setAppInstance(appInstance);
			publisher.setSrcStreamName(liveStreamName());

			publisher.setHostname(config.settings.pushPublish_Host);
			publisher.setPort(config.settings.pushPublish_Port);

			publisher.setDstApplicationName(config.settings.pushPublish_Application);
			publisher.setDstStreamName(config.settings.pushPublish_Key);
			publisher.setDebugLog(false);
			publisher.setImplementation(config.settings.pushPublish_Profile);
			publisher.connect();

			main.info("Begun push publishing");
		} catch (LicensingException ex) {
			ex.printStackTrace();
		}
	}

	public void stopPushPublish() {
		if(publisher != null) {
			publisher.disconnect();
			publisher = null;
			main.info("Stopped push publishing");
		}
	}

	/**
	 * Set stream for preview channel.
	 * @param stream
	 */
	public void switchStream(String stream) {
		config.settings.StreamSwitcher_previewTarget = stream;
		config.save();

		main.info("Switched preview to "+stream);
		playStream(previewStream, previewTarget());
	}

	/**
	 * Copy settings from preview to live.
	 */
	public void publishStream() {
		config.settings.StreamSwitcher_liveTarget = previewTarget();
		config.save();

		main.info("Published stream to live ("+liveTarget()+")");
		playStream(liveStream, liveTarget());
	}

	/**
	 * Republish the currently selected streams. Good for starting up.
	 */
	public void republish() {
		playStream(liveStream, liveTarget());
		playStream(previewStream, previewTarget());
		
		/* restart publishing */
		if(publisher == null) {
			startPushPublish();
		} else {
			publisher.disconnect();
			publisher = null;
			startPushPublish();
		}
	}

	public boolean isPublished(){
		return publisher != null;
	}

	protected boolean playStream(Stream on, String stream) {
		/* sanity check */
		if ( stream == null || stream.equals("") ){
			return false;
		}

		if(on == liveStream) {
			realLiveStream = null;
		} else if(on == previewStream) {
			realPreviewStream = null;
		} else {
			main.info("WHAT IS THIS CRAP?");
		}

		main.info("Playing " + stream + " on " + on.getName() + " stream");

		boolean result = false;
		if(stream.matches(".+:.+")) { // Detect file: "{type}:{filename}"
			result = on.play(stream, 0, -1, true);
			on.setRepeat(true);
		} else {
			result = on.play(stream, -2, -1, true);
			on.setRepeat(false);
		}

		return result;
	}

	private void onPublish(String streamName) {
		main.info("onPublish("+streamName+")");
		if(realLiveStream != null && streamName.trim().equalsIgnoreCase(realLiveStream)) {
			main.info("Live stream "+streamName+" is up again, switching to it.");
			playStream(liveStream, streamName);
		}

		if(realPreviewStream != null && streamName.trim().equalsIgnoreCase(realPreviewStream)) {
			main.info("Preview stream "+streamName+" is up again, switching to it.");
			playStream(previewStream, streamName);
		}
	}

	private void onUnPublish(String streamName) {
		main.info("onUnPublish("+streamName+")");
		streamName = streamName.trim();

		/* if no backup stream is set there is no need to do anything */
		String backupStream = fallbackTarget();
		if ( backupStream == null || backupStream.equalsIgnoreCase(streamName) ){
			return;
		}

		/* record if live target went down */
		if ( streamName.equalsIgnoreCase(liveTarget()) ){
			main.info("Live stream " + streamName + " went down. Switching to backup "+backupStream);
			playStream(liveStream, backupStream);
			realLiveStream = streamName;
		}

		/* record if preview target went down */
		if ( streamName.equalsIgnoreCase(previewTarget()) ){
			main.info("Preview stream " + streamName + " went down. Switching to backup "+backupStream);
			playStream(previewStream, backupStream);
			realPreviewStream = streamName;
		}
	}

	public void onStreamCreate(IMediaStream stream) {
		stream.addClientListener(streamListener);
	}

	private class StreamListener implements IMediaStreamActionNotify {

		@Override
		public void onPublish(IMediaStream stream, String streamName,
				boolean isRecord, boolean isAppend) {
			StreamSwitcher.this.onPublish(streamName);
		}

		@Override
		public void onUnPublish(IMediaStream stream, String streamName,
				boolean isRecord, boolean isAppend) {
			StreamSwitcher.this.onUnPublish(streamName);
		}

		@Override
		public void onPause(IMediaStream stream, boolean isPause,
				double location) { }

		@Override
		public void onPlay(IMediaStream stream, String streamName,
				double playStart, double playLen, int playReset) { }


		@Override
		public void onSeek(IMediaStream stream, double location) { }

		@Override
		public void onStop(IMediaStream stream) { }

	}
}
