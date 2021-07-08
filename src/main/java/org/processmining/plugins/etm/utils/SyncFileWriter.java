package org.processmining.plugins.etm.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SyncFileWriter {

	private FileWriter fWriter;
	private String path;
	private File file;

	
	public SyncFileWriter(String noiseLevel) {
		this("C:\\Users\\qnoxo\\workshopEclipse\\2015 Alignment Repair\\experiments\\", noiseLevel);
	}
	
	public SyncFileWriter(String _path, String noiseLevel) {
		long expStart = System.currentTimeMillis();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm");
		String date = dateFormat.format(new Date(expStart));
		this.path = _path+date+"_"+noiseLevel+".csv";
		this.file = new File(path);
	}

	public void write(String string) {
		synchronized (this) {
			try {
				this.fWriter = new FileWriter(file.getAbsoluteFile(), true);
				fWriter.write(string);
				fWriter.write("\r\n");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (fWriter != null) {
					try {
						this.fWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	//	this.write = new PrintWriter("D:\\SoftLearnData\\models\\LogExamplesXML\\Logs\\a22\\Nueva carpeta\\test1.txt", "UTF-8");

}
