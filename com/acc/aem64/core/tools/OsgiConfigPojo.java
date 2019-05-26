package com.acc.aem64.core.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.Configuration;

public class OsgiConfigPojo {

	private String pid;
	private String factoryPid;
	private String bundleLocation;
	private Long changeCount;
	private Map<String, Object> properties;
	private List<String> repositoryPaths = new ArrayList<String>();

	public OsgiConfigPojo(Configuration conf, List<String> repositoryPaths) {
		this.pid = conf.getPid();
		this.factoryPid = conf.getFactoryPid();
		this.bundleLocation = conf.getBundleLocation();
		this.changeCount = conf.getChangeCount();
		this.properties = getMap(conf.getProperties());
		this.repositoryPaths = repositoryPaths.size() > 0 ? repositoryPaths : null;
	}

	private Map<String, Object> getMap(Dictionary<String, Object> dict) {
		List<String> keys = Collections.list(dict.keys());
		Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
		dictCopy.remove("service.factoryPid");
		dictCopy.remove("service.pid");
		return dictCopy;
	}
}