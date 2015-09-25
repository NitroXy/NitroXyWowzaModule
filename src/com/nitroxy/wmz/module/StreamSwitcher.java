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
		main.info("StreamSwitcher creating streams live="+config.settings.StreamSwitcher_liveStream + ", preview=" + config.settings.StreamSwitcher_previewStream);
		liveStream = Stream.createInstance(appInstance, config.settings.StreamSwitcher_liveStream);
		previewStream = Stream.createInstance(appInstance, config.settings.StreamSwitcher_previewStream);
		
		/* restore streams if possible */
		if ( !playStream(previewStream, config.settings.StreamSwitcher_previewTarget) ){
			realPreviewStream = config.settings.StreamSwitcher_previewTarget;
		}
		if ( !playStream(liveStream, config.settings.StreamSwitcher_liveTarget) ){
			realLiveStream = config.settings.StreamSwitcher_liveTarget;
		}
	}
	
	public void close(){
		main.info("StreamSwitcher::close()");
		stopPushPublish();
		liveStream.close();
		previewStream.close();
	}
	
	public void startPushPublish() {
		try {
			publisher = new PushPublishRTMP();
			publisher.setAppInstance(appInstance);
			publisher.setSrcStreamName(config.settings.StreamSwitcher_liveStream);
			
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
	
	/**
	 * Set stream for preview export
	 * @param stream
	 */
	public void switchStream(String stream) {
		config.settings.StreamSwitcher_previewTarget = stream;
		
		playStream(previewStream, config.settings.StreamSwitcher_previewTarget);
		
		main.info("Switched preview to "+stream);
		config.save();
	}
	
	/**
	 * Copy settings from preview to live
	 */
	public void publishStream() {
		config.settings.StreamSwitcher_liveTarget = config.settings.StreamSwitcher_previewTarget;
		playStream(liveStream, config.settings.StreamSwitcher_liveTarget);
		main.info("Published stream to live ("+config.settings.StreamSwitcher_liveTarget+")");
		config.save();
		if(publisher == null) {
			startPushPublish();
		}
	}
	
	/**
	 * Republish the currently selected streams. Good for starting up.
	 */
	public void republish() {
		playStream(liveStream, config.settings.StreamSwitcher_liveTarget);
		playStream(previewStream, config.settings.StreamSwitcher_previewTarget);
		if(publisher == null) {
			startPushPublish();
		} else {
			publisher.disconnect();
			publisher = null;
			startPushPublish();
		}
	}
	
	public void stopPushPublish() {
		if(publisher != null) {
			publisher.disconnect();
			publisher = null;
			main.info("Stopped push publishing");
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
		String backupStream = config.settings.StreamSwitcher_fallbackStream;
		if ( backupStream == null || backupStream.equalsIgnoreCase(streamName) ){
			return;
		}
		
		/* record if live target went down */
		if ( streamName.equalsIgnoreCase(config.settings.StreamSwitcher_liveTarget) ){
			main.info("Live stream " + streamName + " went down. Switching to backup "+backupStream);
			playStream(liveStream, backupStream);
			realLiveStream = streamName;
		}

		/* record if preview target went down */
		if ( streamName.equalsIgnoreCase(config.settings.StreamSwitcher_previewTarget) ){
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
