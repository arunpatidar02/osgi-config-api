package com.acc.aem64.core.tools;

public class RepoConfigPojo {

    private String path;
    private String pid;
    
	public RepoConfigPojo(String path){
		this.path = path;
		this.pid = getPid(path);
	}
	public String getPid(String path){
		String pid = path.substring(path.lastIndexOf("/")+1);
		if(pid.indexOf("-")>0)
	    	pid = pid.substring(0, pid.indexOf("-"));
		
		return pid; 
	}
	public String getPath() {
		return path;
	}	
}
