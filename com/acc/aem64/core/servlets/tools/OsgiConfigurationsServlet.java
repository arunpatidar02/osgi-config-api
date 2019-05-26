package com.acc.aem64.core.servlets.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acc.aem64.core.tools.OsgiConfigPojo;
import com.acc.aem64.core.tools.RepoConfigPojo;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=API to get osgi config",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/api/osgiconfig",
		"sling.servlet.extensions=" + "json" })
public class OsgiConfigurationsServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 2598426539166789516L;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Reference
	private ConfigurationAdmin configAdmin;

	@Reference
	private QueryBuilder queryBuilder;

	@Override
	protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
			throws ServerException, IOException {
		resp.setContentType("application/json");
		PrintWriter pw = resp.getWriter();
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		try {
			String q = req.getParameter("q");
			String type = req.getParameter("type");
			String repoParam = req.getParameter("repo");
			String filter = null;
			boolean param = true;
			String errMsg = "No result found, please check query parameters value";

			if (q != null && type != null) {
				if (type.equalsIgnoreCase("pid")) {
					filter = "(service.pid=" + q + ")";
				} else if (type.equalsIgnoreCase("fid")) {
					filter = "(service.factoryPid=" + q + ")";
				} else if (!(type.equalsIgnoreCase("pid") || type.equalsIgnoreCase("fid"))) {
					param = false;
					errMsg = "invalid type paramaeter value. It should be 'pid' or 'fid'";
				}
			} else if (q != null && type == null) {
				param = false;
				errMsg = "type parameter is missing";
			} else if (q == null && type != null) {
				param = false;
				errMsg = "q parameter is missing";
			}

			if (param) {
				Configuration[] configurations = null;
				List<OsgiConfigPojo> confObjList = new ArrayList<OsgiConfigPojo>();
				List<RepoConfigPojo> repoConfigObjList = new ArrayList<RepoConfigPojo>();
				List<String> repoConfigPidList = new ArrayList<String>();

				configurations = configAdmin.listConfigurations(filter);
				if (configurations != null) {
					updateRepoConfigList(req, repoConfigObjList, repoConfigPidList);
					confObjList = getConfigurations(req, configurations, repoParam, repoConfigObjList,
							repoConfigPidList);
				}
				if (confObjList.size() > 0) {
					pw.write(gson.toJson(confObjList));
				} else {
					pw.write(gson.toJson(new String("No configuration found for this request")));
				}
			} else {
				pw.write(gson.toJson(new String(errMsg)));
			}
		} catch (Exception e) {
			logger.error("[doGet] : " + e.getMessage());
			e.printStackTrace();
			pw.write(gson.toJson(new String(e.getMessage())));
		} finally {
			pw.close();
		}
	}

	private List<OsgiConfigPojo> getConfigurations(SlingHttpServletRequest req, Configuration[] configurations,
			String repoParam, List<RepoConfigPojo> repoConfigObjList, List<String> repoConfigPidList) {
		List<OsgiConfigPojo> confObjList = new ArrayList<OsgiConfigPojo>();
		for (int i = 0; i < configurations.length; i++) {
			Configuration c = configurations[i];
			boolean isRepo = false;
			List<String> repoPath = new ArrayList<String>();
			if (c.getFactoryPid() != null && repoConfigPidList.contains(c.getFactoryPid())) {
				isRepo = isRepoConfigPojo(req, c, repoConfigObjList, repoPath);
			} else if (c.getFactoryPid() == null && repoConfigPidList.contains(c.getPid())) {
				isRepo = true;
			}

			OsgiConfigPojo conf = new OsgiConfigPojo(configurations[i], repoPath);

			if (repoParam != null) {
				if (repoParam.equalsIgnoreCase("true") && isRepo) {
					confObjList.add(conf);
				} else if (repoParam.equalsIgnoreCase("false") && !isRepo) {
					confObjList.add(conf);
				} else if (!(repoParam.equalsIgnoreCase("true") || repoParam.equalsIgnoreCase("false"))) {
					confObjList.add(conf);
				}
			} else {
				confObjList.add(conf);
			}
		}
		return confObjList;
	}

	private boolean isRepoConfigPojo(SlingHttpServletRequest req, Configuration c,
			List<RepoConfigPojo> repoConfigObjList, List<String> repoPath) {
		HashMap<String, Object> webConfigPropMap = new HashMap<String, Object>(getMap(c.getProperties()));
		Set<String> webConfigPropKeys = webConfigPropMap.keySet();
		boolean mapsEqual;
		int confCtr = 0;

		Iterator<RepoConfigPojo> it = repoConfigObjList.iterator();
		while (it.hasNext()) {
			RepoConfigPojo rc = it.next();
			if (rc.getPid(rc.getPath()).equalsIgnoreCase(c.getFactoryPid())) {
				HashMap<String, Object> repoConfigPropMap = new HashMap<String, Object>();
				ValueMap valueMap = req.getResourceResolver().getResource(rc.getPath()).getValueMap();
				mapsEqual = true;

				for (Entry<String, Object> e : valueMap.entrySet()) {
					String key = e.getKey();
					if (webConfigPropKeys.contains(key)) {
						Object value = e.getValue();
						repoConfigPropMap.put(key, value);
					}
				}

				for (Entry<String, Object> e : webConfigPropMap.entrySet()) {
					try {
						String key = e.getKey();
						if (!webConfigPropMap.get(key).getClass().isArray()
								&& !webConfigPropMap.get(key).equals(repoConfigPropMap.get(key))) {
							mapsEqual = false;
							break;
						} else if (webConfigPropMap.get(key).getClass().isArray()
								&& !Arrays.toString((String[]) webConfigPropMap.get(key))
										.equals(Arrays.toString((String[]) repoConfigPropMap.get(key)))) {
							mapsEqual = false;
							break;
						}
					} catch (Exception e1) {
						logger.error("[isRepoConfigPojo] : " + e1.getMessage());
					}
				}
				if (mapsEqual) {
					confCtr++;
					repoPath.add(rc.getPath());
				}
			}
		}
		return confCtr > 0;
	}

	private Map<String, Object> getMap(Dictionary<String, Object> dict) {
		List<String> keys = Collections.list(dict.keys());
		Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
		dictCopy.remove("service.factoryPid");
		dictCopy.remove("service.pid");
		return dictCopy;
	}

	private void updateRepoConfigList(final SlingHttpServletRequest req, List<RepoConfigPojo> repoConfigObjList,
			List<String> repoConfigPidList) throws RepositoryException {
		SearchResult result = null;
		final Map<String, String> qmap = new HashMap<String, String>();
		qmap.put("type", "sling:OsgiConfig");
		qmap.put("group.1_group.1_path", "/libs/system");
		qmap.put("group.1_group.2_path", "/apps");
		qmap.put("group.1_group.p.or", "true");
		qmap.put("p.limit", "-1");
		Query query = queryBuilder.createQuery(PredicateGroup.create(qmap),
				req.getResourceResolver().adaptTo(Session.class));
		try {
			result = query.getResult();
			for (final Hit hit : result.getHits()) {
				String path = hit.getPath();
				RepoConfigPojo rc = new RepoConfigPojo(path);
				repoConfigObjList.add(rc);
				repoConfigPidList.add(rc.getPid(path));
			}
		} catch (Exception e) {
			logger.error("[updateRepoConfigList] : " + e.getMessage());
		}
	}

}
