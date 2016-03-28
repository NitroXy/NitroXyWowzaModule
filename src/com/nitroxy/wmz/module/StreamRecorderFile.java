package com.nitroxy.wmz.module;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderFileVersionDelegate;

class StreamRecorderFile implements IStreamRecorderFileVersionDelegate {

	@Override
	public String getFilename(IStreamRecorder recorder) {
		final File file = new File(recorder.getBaseFilePath());
		return file.getParent() + "/" + recorder.getStreamName() + "_" + getDateString() + ".mp4";
	}
	
	protected String getDateString(){
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date now = Calendar.getInstance().getTime();
		return formatter.format(now );
	}

}
