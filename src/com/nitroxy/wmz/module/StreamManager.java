package com.nitroxy.wmz.module;

import com.torandi.net.command.JSONCommand;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.bootstrap.Bootstrap;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.mediacaster.IMediaCaster;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.stream.publish.Stream;

public class StreamManager implements IMediaStreamNameAliasProvider {

	private Config config = null;
	private String liveStream = null;
	private String previewStream = null;
	private String bindAddress = "localhost";
	private int bindPort = 1337;
	private NitroXyModule main;
	
	private String liveStreamTarget = "wowza_test.stream";
	
	private String contentPath;
	
	private JSONCommand<StreamManager> command;
	
	public StreamManager(NitroXyModule main, IApplicationInstance appInstance) {
		this.main = main;
		contentPath =  Bootstrap.getServerHome(Bootstrap.CONFIGHOME) + "/content/";
		main.info("Content path: "+contentPath);
		
		WMSProperties props = appInstance.getProperties();
		for(String s : props.getAllAsStrings()) {
			main.info(s);
		}
		
		bindAddress = props.getPropertyStr("NitroXyBindAddress", "localhost");
		bindPort = props.getPropertyInt("NitroXyBindPort", 1337);
		
	
		liveStream = props.getPropertyStr("NitroXyLiveStream", "live");
		previewStream = props.getPropertyStr("NitroXyPreviewStream", "preview");

		main.info("Application: "+appInstance.getApplication().getName() + ". Live: "+liveStream + ". Preview:" + previewStream);

		command = new JSONCommand<StreamManager>(main, this, bindAddress, bindPort);
		
		main.info("Set up NitroXy Stream Control for application "+appInstance.getApplication().getName());
	}
	
	@Override
	protected void finalize() throws Throwable {
		command.shutdown();
		super.finalize();
	}

	public String resolveStreamName(String name) {
		main.info("Stream request: "+name);
		if(name.equalsIgnoreCase(liveStream)) {
			main.info("Is live stream, redirecting to "+liveStreamTarget);
			return liveStreamTarget;
		} else {
			return name;
		}
	}

	public String resolvePlayAlias(IApplicationInstance appInstance, String name) {
		return resolveStreamName(name);
	}

	public String resolveStreamAlias(IApplicationInstance appInstance,
			String name) {
		if(name.endsWith(".stream")) {
			return Utils.readFile(contentPath + name);
		} else {
			return name;
		}
	}
}
