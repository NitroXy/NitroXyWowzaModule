package com.nitroxy.wmz.module;

import com.wowza.wms.logging.*;
import com.wowza.wms.server.*;
import com.wowza.wms.vhost.*;

public class ServerListener implements IServerNotify2 {
	public void onServerInit(IServer server) {
		WMSLoggerFactory.getLogger(null).info("NitroXy::onServerInit");
		IVHost vhost = VHostSingleton.getInstance(VHost.VHOST_DEFAULT);
		vhost.startApplicationInstance("nitroxy");
	}
	
	public void onServerConfigLoaded(IServer server) {
		
	}

	public void onServerCreate(IServer server) {

	}


	public void onServerShutdownStart(IServer server) {
		
	}

	public void onServerShutdownComplete(IServer server) {

	}
}
