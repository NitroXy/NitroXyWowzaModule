package com.nitroxy.wmz.module;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.pushpublish.protocol.rtmp.PushPublishRTMP;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.publish.Stream;

public class StreamSwitcher {
	private Stream liveStream = null;
	private Stream previewStream = null;
	
	private PushPublishRTMP publisher = null;
	
	private NitroXyModule main;
	
	private Config config;
	
	public StreamSwitcher(NitroXyModule main, IApplicationInstance appInstance, Config config) throws LicensingException {
		this.main = main;
		this.config = config;
		
		liveStream = Stream.createInstance(appInstance, config.settings.StreamSwitcher_liveStream);

		if(config.settings.StreamSwitcher_liveTarget != null && !config.settings.StreamSwitcher_liveTarget.equals("")) {
			playStream(liveStream, config.settings.StreamSwitcher_liveTarget);
			main.info("Started "+config.settings.StreamSwitcher_liveTarget + " on live stream");
		}
		
		previewStream = Stream.createInstance(appInstance, config.settings.StreamSwitcher_previewStream);

		if(config.settings.StreamSwitcher_previewStream != null && !config.settings.StreamSwitcher_previewStream.equals("")) {
			playStream(liveStream, config.settings.StreamSwitcher_previewStream);
			main.info("Started "+config.settings.StreamSwitcher_previewTarget + " on preview stream");
		}
		
		main.info("Application: "+appInstance.getApplication().getName() + ". Live: "+config.settings.StreamSwitcher_liveStream + ". Preview:" + config.settings.StreamSwitcher_previewStream);

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
