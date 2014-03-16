package com.nitroxy.wmz.module;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.pushpublish.protocol.rtmp.PushPublishRTMP;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.publish.Stream;

public class StreamSwitcher {
	private Stream liveStream = null;
	private Stream previewStream = null;
	
	private PushPublishRTMP publisher = null;
	
	private NitroXyModule main;
	
	private Config config;
	private IApplicationInstance appInstance;
	
	public StreamSwitcher(NitroXyModule main, IApplicationInstance appInstance, Config config) {
		this.main = main;
		this.config = config;
		this.appInstance = appInstance;
		
		liveStream = Stream.createInstance(appInstance, config.settings.StreamSwitcher_liveStream);

		previewStream = Stream.createInstance(appInstance, config.settings.StreamSwitcher_previewStream);
		previewStream.setRepeat(true);
		
		if(config.settings.StreamSwitcher_previewStream != null && !config.settings.StreamSwitcher_previewStream.equals("")) {
			playStream(liveStream, config.settings.StreamSwitcher_previewStream);
			main.info("Started "+config.settings.StreamSwitcher_previewTarget + " on preview stream");
		}
		
		main.info("Application: "+appInstance.getApplication().getName() + ". Live: "+config.settings.StreamSwitcher_liveStream + ". Preview:" + config.settings.StreamSwitcher_previewStream);
	}
	
	public void startPushPublish(IApplicationInstance appInstance) {
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
			startPushPublish(appInstance);
		}
	}
	
	/**
	 * Republish the currently selected streams. Good for starting up.
	 */
	public void republish() {
		playStream(liveStream, config.settings.StreamSwitcher_liveTarget);
		playStream(previewStream, config.settings.StreamSwitcher_previewTarget);
		if(publisher == null) {
			startPushPublish(appInstance);
		}
	}
	
	private void playStream(Stream on, String stream) {
		if(stream.matches(".+:.+")) { // Detect file: "{type}:{filename}"
			on.play(stream, 0, -1, true);
			on.setRepeat(true);
		} else {
			on.play(stream, -2, -1, true);
			on.setRepeat(false);
		}
	}
}
