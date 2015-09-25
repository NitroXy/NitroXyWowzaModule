package com.nitroxy.wmz.module;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.torandi.net.command.Exposed;
import com.torandi.net.command.JSONCommand;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderSimpleFileVersionDelegate;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;

public class ApplicationManager {
	private Config config;
	private StreamSwitcher streamSwitcher = null;
	private NitroXyModule main;
	private IApplicationInstance appInstance;
	private IVHost vhost;
	
	JSONCommand<ApplicationManager> command;
	
	public ApplicationManager(NitroXyModule main, IApplicationInstance appInstance) throws LicensingException {
		this.appInstance = appInstance;
		this.main = main;
		this.vhost = appInstance.getVHost();

		config = Config.getConfig(appInstance.getApplication().getName(), main);
		if(config.exists()) {
			if(config.settings.StreamSwitcher_Enabled) {
				streamSwitcher = new StreamSwitcher(main, appInstance, config);
			}
			command = new JSONCommand<ApplicationManager>(main, this, config.settings.Control_Address, config.settings.Control_Port);
		}

		startRecording();
	}

	public void stop() {
		if ( streamSwitcher != null ){
			streamSwitcher.close();
		}

		stopRecording();
		command.close();
	}

	protected void startRecording(){
		StreamRecorderParameters record = new StreamRecorderParameters(this.appInstance);
		record.segmentationType = IStreamRecorderConstants.SEGMENT_NONE;
		record.fileVersionDelegate = new StreamRecorderSimpleFileVersionDelegate();
		vhost.getLiveStreamRecordManager().startRecording(appInstance, record);
	}
	
	protected void stopRecording(){
		vhost.getLiveStreamRecordManager().stopRecording(appInstance);
	}

	protected String liveStreamName(){
		return config.settings.StreamSwitcher_liveStream;
	}

	protected String previewStreamName(){
		return config.settings.StreamSwitcher_previewStream;
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
		Pattern p = Pattern.compile("^\\[.+\\].+");

		ArrayList<String> streams = new ArrayList<String>();
		for(IMediaStream s : appInstance.getStreams().getStreams()) {
			String name = s.getName();
			Matcher m = p.matcher(name);

			if ( name.equalsIgnoreCase(liveStreamName()) ) continue;
			if ( name.equalsIgnoreCase(previewStreamName()) ) continue;
			if ( m.matches() ) continue;

			streams.add(name);
		}
		
		/* list content */
		File contentDir = new File(appInstance.getStreamStorageDir());
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
	
	public void onStreamCreate(IMediaStream stream) {
		if(streamSwitcher != null) streamSwitcher.onStreamCreate(stream);
	}
	
	@Exposed
	public Map<String,Object> fullStatus(){
		Map<String,Object> status = new Hashtable<String,Object>();
		status.put("live_target", currentLive());
		status.put("preview_target", currentPreview());
		status.put("fallback_target", currentFallback());
		status.put("is_published", isPublished() ? "yes" : "no");

		/* get all current recordings */
		Map<String,String> recordings = new Hashtable<String,String>();
		for ( IStreamRecorder recorder: vhost.getLiveStreamRecordManager().getRecordersList(appInstance) ){
			recordings.put(recorder.getStreamName(), recorder.getCurrentFile());
		}
		status.put("recording", recordings);

		return status;
	}
	
	@Exposed
	public String currentLive() {
		return config.settings.StreamSwitcher_liveTarget;
	}
	
	@Exposed
	public String currentPreview() {
		return config.settings.StreamSwitcher_previewTarget;
	}
	
	@Exposed
	public void setFallback(String fallbackStream) {
		config.settings.StreamSwitcher_fallbackStream = fallbackStream;
		config.save();
	}
	
	@Exposed
	public String currentFallback() {
		return config.settings.StreamSwitcher_fallbackStream;
	}
	
	public boolean isPublished(){
		if ( streamSwitcher != null ){
			return streamSwitcher.isPublished();
		} else {
			return false;
		}
	}
	
	@Exposed
	public boolean republish() {
		if(streamSwitcher != null) {
			streamSwitcher.republish();
			return true;
		} else
			return false;
	}
	
	@Exposed
	public void stopPushPublish() {
		if(streamSwitcher != null)
			streamSwitcher.stopPushPublish();
	}
	
	@Exposed
	public void startPushPublish() {
		if(streamSwitcher != null)
			streamSwitcher.stopPushPublish();
	}

	@Exposed
	public boolean segment(){
		main.info("Segmenting live stream recording - " + currentLive());
		vhost.getLiveStreamRecordManager().splitRecording(appInstance, currentLive());
		return true;
	}
}
