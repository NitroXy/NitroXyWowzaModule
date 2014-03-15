package com.nitroxy.wmz.module;

import com.torandi.net.Logger;
import com.wowza.wms.application.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.mediacaster.IMediaCaster;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.server.IServer;
import com.wowza.wms.server.IServerNotify;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;

public class NitroXyModule extends ModuleBase implements IMediaStreamNameAliasProvider2, Logger {
	private NitroXyInterface iface = null;
	Config config = null;
	String live_name = null;
	String preview_name = null;
	String bind_address = "localhost";
	int bind_port = 1337;
	
	public void onAppStart(IApplicationInstance appInstance) {
		WMSProperties props = appInstance.getApplication().getProperties();

		bind_address = props.getPropertyStr("NitroXyBindAddress", "localhost");
		bind_port = props.getPropertyInt("NitroXyBindPort", 1337);
		live_name = props.getPropertyStr("NitroXyLiveStream");
		preview_name = props.getPropertyStr("NitroXyPreviewStream");
		
		if(live_name == null) error("Missing property NitroXyLiveStream");
		if(preview_name == null) error("Missing property NitroXyPreviewStream");
		
		info("Application: "+appInstance.getApplication().getName() + ". Live: "+live_name + ". Preview:" + preview_name);

		iface = new NitroXyInterface(this);
		
		appInstance.setStreamNameAliasProvider(this);
		info("Set up NitroXy control for application "+appInstance.getApplication().getName());
	}

	public void onAppStop(IApplicationInstance appInstance) {
		iface.shutdown();
	}

	public void onConnect(IClient client, RequestFunction function,
			AMFDataList params) {
	}

	public void onConnectAccept(IClient client) {
	}

	public void onConnectReject(IClient client) {
	}

	public void onDisconnect(IClient client) {
	}

	public void onStreamCreate(IMediaStream stream) {
	}

	public void onStreamDestroy(IMediaStream stream) {
	}
	
	@Override
	public void onServerInit(IServer server) {
	
		
		iface.start();
	}

	@Override
	public void onServerShutdownStart(IServer server) {
		iface.shutdown();
	}

	public void info(String msg) {
		getLogger().info("NitroXy: "+msg);
	}

	public void error(String msg) {
		getLogger().error("NitroXy: "+msg);
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name) {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance,
			String name) {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance,
			String name, IClient client) {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance,
			String name, IHTTPStreamerSession httpSession) {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance,
			String name, RTPSession rtpSession) {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance,
			String name, ILiveStreamPacketizer liveStreamPacketizer) {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance,
			String name, IMediaCaster mediaCaster) {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public void onServerCreate(IServer server) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onServerShutdownComplete(IServer server) {
		// TODO Auto-generated method stub
		
	}

}