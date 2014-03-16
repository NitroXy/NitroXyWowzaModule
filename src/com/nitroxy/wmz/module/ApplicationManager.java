package com.nitroxy.wmz.module;

import com.torandi.net.command.Exposed;
import com.torandi.net.command.JSONCommand;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.server.LicensingException;

public class ApplicationManager {
	private Config config;
	private StreamSwitcher streamSwitcher = null;
	private NitroXyModule main;
	private IApplicationInstance appInstance;
	
	JSONCommand<ApplicationManager> command;
	
	public ApplicationManager(NitroXyModule main, IApplicationInstance appInstance) throws LicensingException {
		this.appInstance = appInstance;
		config = Config.getConfig(appInstance.getApplication().getName(), main);
		if(config.exists()) {
			if(config.settings.StreamSwitcher_Enabled) {
				streamSwitcher = new StreamSwitcher(main, appInstance, config);
			}
			command = new JSONCommand<ApplicationManager>(main, this, config.settings.Control_Address, config.settings.Control_Port);
		}
		
	}
	
	public boolean enabled() {
		return config.exists();
	}
	
	@Exposed
	public boolean switchStream(String stream)  {
		if(streamSwitcher != null) {
			streamSwitcher.switchStream(stream);
			return true;
		} else
			return false;
	}
	
	@Exposed
	public boolean publishStream() {
		if(streamSwitcher != null) {
			streamSwitcher.publishStream();
			return true;
		} else
			return false;
	}
	
	@Exposed
	public String currentLive() {
		return config.settings.StreamSwitcher_liveTarget;
	}
	
	@Exposed
	public String currentPreview() {
		return config.settings.StreamSwitcher_previewTarget;
	}
}
