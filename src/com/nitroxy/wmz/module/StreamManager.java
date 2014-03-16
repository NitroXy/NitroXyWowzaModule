package com.nitroxy.wmz.module;

import com.torandi.net.command.Exposed;
import com.torandi.net.command.JSONCommand;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.bootstrap.Bootstrap;
import com.wowza.wms.pushpublish.protocol.rtmp.PushPublishRTMP;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.publish.Stream;

public class StreamManager {

	private Config config = null;
	private String liveStreamName = null;
	private String previewStreamName = null;
	private String bindAddress = "localhost";
	private int bindPort = 1337;
	private String pushPublishHost;
	private String pushPublishProfile;
	private String pushPublishKey;
	private String pushPublishApplication;
	
	private Stream liveStream = null;
	
	private Stream fileStream = null;
	private PushPublishRTMP publisher = null;
	
	private NitroXyModule main;
	
	private String liveStreamTarget = "preroll";
	private String previewStreamTarget = null;
	
	private String contentPath;
	
	private JSONCommand<StreamManager> command;
	
	public StreamManager(NitroXyModule main, IApplicationInstance appInstance) throws LicensingException {
		this.main = main;
		contentPath =  Bootstrap.getServerHome(Bootstrap.CONFIGHOME) + "/content/";
		main.info("Content path: "+contentPath);
		
		WMSProperties props = appInstance.getProperties();
		for(String s : props.getAllAsStrings()) {
			main.info(s);
		}
		
		bindAddress = props.getPropertyStr("NitroXyBindAddress", "localhost");
		bindPort = props.getPropertyInt("NitroXyBindPort", 1337);
		
		fileStream = Stream.createInstance(appInstance, "preroll");
		fileStream.play("mp4:af.mov",0, -1, true);
		fileStream.setRepeat(true);
	
		liveStreamName = props.getPropertyStr("NitroXyLiveStream", "live");
		previewStreamName = props.getPropertyStr("NitroXyPreviewStream", "preview");
		
		
		liveStream = Stream.createInstance(appInstance, liveStreamName);
		liveStream.play(liveStreamTarget, -2, -1, true);

		main.info("Application: "+appInstance.getApplication().getName() + ". Live: "+liveStreamName + ". Preview:" + previewStreamName);

		command = new JSONCommand<StreamManager>(main, this, bindAddress, bindPort);
		
		main.info("Set up NitroXy Stream Control for application "+appInstance.getApplication().getName());
		

		pushPublishProfile = props.getPropertyStr("NitroXyPushPublishProfile");
		pushPublishKey = props.getPropertyStr("NitroXyPushPublishKey");
		pushPublishHost = props.getPropertyStr("NitroXyPushPublishHost");
		pushPublishApplication = props.getPropertyStr("NitroXyPushPublishApplication");

		if(pushPublishApplication == null || pushPublishKey == null || pushPublishHost == null || pushPublishProfile == null ) {
			main.error("Missing push publish settings");
		} else {
			publisher = new PushPublishRTMP();
			publisher.setAppInstance(appInstance);
			publisher.setSrcStreamName(liveStreamName);
			
			publisher.setHostname(pushPublishHost);
			/* TODO: port */
			publisher.setDstApplicationName(pushPublishApplication);
			publisher.setDstStreamName(pushPublishKey);
			publisher.setDebugLog(false);
			publisher.setImplementation(pushPublishProfile);
			publisher.connect();
			main.info("Publishing!");
			
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		command.shutdown();
		super.finalize();
	}
	
	@Exposed
	public void switchStream(String stream) {
		liveStreamTarget = stream;
		liveStream.play(stream, -2, -1, true);
		main.info("Switched stream to "+stream);
	}
}
