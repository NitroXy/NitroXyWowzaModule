package com.nitroxy.wmz.module;

import java.util.HashMap;

import com.torandi.net.Logger;
import com.torandi.net.command.JSONCommand;
import com.wowza.wms.application.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.*;

public class NitroXyModule extends ModuleBase implements Logger {
	private HashMap<IApplicationInstance, ApplicationManager> managers = new HashMap<IApplicationInstance, ApplicationManager>();
	
	public void onAppStart(IApplicationInstance appInstance) throws LicensingException {
		ApplicationManager mngr = new ApplicationManager(this, appInstance);
		managers.put(appInstance, mngr);
	}

	public void onAppStop(IApplicationInstance appInstance) {
		managers.remove(appInstance);
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
	
	public void info(String msg) {
		getLogger().info("NitroXy: "+msg);
	}

	public void error(String msg) {
		getLogger().error("NitroXy: "+msg);
	}

}