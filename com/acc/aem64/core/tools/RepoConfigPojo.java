package com.acc.aem64.core.tools;

public class RepoConfigPojo {

    private String path;
    private String pid;
    
	public RepoConfigPojo(String path){
		this.path = path;
		this.pid = getPid(path);
	}
	public String getPid(String path){
		String pid = "";
		if(path.lastIndexOf("-")>0)
	    	pid = path.substring(path.lastIndexOf("/")+1, path.lastIndexOf("-"));
	    else
	    	pid = path.substring(path.lastIndexOf("/")+1);

		return pid; 
	}
	public String getPath() {
		return path;
	}	
}