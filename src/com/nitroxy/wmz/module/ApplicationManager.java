package com.nitroxy.wmz.module;

import java.io.File;
import java.util.ArrayList;

import com.torandi.net.command.Exposed;
import com.torandi.net.command.JSONCommand;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.bootstrap.Bootstrap;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.IMediaStream;

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
	public ArrayList<String> getStreams() {
		ArrayList<String> streams = new ArrayList<String>();
		for(IMediaStream s : appInstance.getStreams().getStreams()) {
			if(!s.getName().equalsIgnoreCase(config.settings.StreamSwitcher_liveStream)
					&& !s.getName().equalsIgnoreCase(config.settings.StreamSwitcher_previewStream))
			streams.add(s.getName());
		}
		
		/* list content */
		File contentDir = new File(Bootstrap.getServerHome(Bootstrap.CONFIGHOME) + "/content");
		for(final File fileEntry : contentDir.listFiles()) {
			if(!fileEntry.isDirectory()) {
				String ext = Utils.fileExtension(fileEntry.getName());
				if(ext.equalsIgnoreCase("mp4")
						|| ext.equalsIgnoreCase("mov")
						|| ext.equalsIgnoreCase("3gp")
						|| ext.equalsIgnoreCase("m4v")) {
					streams.add("mp4:"+fileEntry.getName());
				} else if(ext.equalsIgnoreCase("flv")) {
					streams.add("flv:"+fileEntry.getName());	
				} else if(ext.equalsIgnoreCase("mp3")) {
					streams.add("mp3:"+fileEntry.getName());	
				}
			}
		}
		
		return streams;
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
