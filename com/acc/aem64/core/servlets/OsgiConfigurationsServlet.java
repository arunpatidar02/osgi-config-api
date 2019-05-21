package com.acc.aem64.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.acc.aem64.core.pojo.OsgiConfigPojo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=API to get osgi config",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/api/osgi-config",
		"sling.servlet.extensions=" + "json" })
public class OsgiConfigurationsServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 2598426539166789516L;

	@Reference
	private ConfigurationAdmin configAdmin;

	@Override
	protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
			throws ServerException, IOException {
		resp.setContentType("application/json");
		PrintWriter pw = resp.getWriter();
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		try {
			String q = req.getParameter("q");
			String type = req.getParameter("type");
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
				configurations = configAdmin.listConfigurations(filter);
				if (configurations != null) {
					confObjList = getConfigurations(configurations);
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
			e.printStackTrace();
			pw.write(gson.toJson(new String(e.getMessage())));
		} finally {
			resp.getWriter().close();
		}
	}

	private List<OsgiConfigPojo> getConfigurations(Configuration[] configurations) {
		List<OsgiConfigPojo> confObjList = new ArrayList<OsgiConfigPojo>();
		for (int i = 0; i < configurations.length; i++) {
			OsgiConfigPojo conf = new OsgiConfigPojo(configurations[i]);
			confObjList.add(conf);
		}
		return confObjList;
	}
}
