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
		try {

			PrintWriter pw = resp.getWriter();
			Configuration[] configurations = null;
			Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
			List<OsgiConfigPojo> confObjList = new ArrayList<OsgiConfigPojo>();
			
			resp.setContentType("application/json");

			String mode = req.getParameter("mode");
			if (mode != null) {
				String filter = null;
				if (mode.equalsIgnoreCase("all")) {
					configurations = configAdmin.listConfigurations(filter);
				} else if (mode.equalsIgnoreCase("search") && req.getParameter("q") != null) {
					String q = req.getParameter("q");
					String type = req.getParameter("type");
					if (type != null && type.equalsIgnoreCase("pid")) {
						filter = "(service.pid=" + q + ")";
					} else if (type != null && type.equalsIgnoreCase("fid")) {
						filter = "(service.factoryPid=" + q + ")";
					}
					configurations = configAdmin.listConfigurations(filter);
				}
			}

			if (configurations != null) {
				confObjList = getConfigurations(configurations);
				String jsonInString = gson.toJson(confObjList);
				pw.print(jsonInString);
			} else {
				String jsonInString = gson.toJson(new String("Something is wrong..., Please check query string parameters"));
				pw.print(jsonInString);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
