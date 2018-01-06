/**
 * 
 */
package com.synectiks.commons.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.dynamodb.Entity;
import com.synectiks.commons.entities.oak.OakFileNode;

/**
 * @author Rajesh
 */
public interface IUtils {

	Logger logger = LoggerFactory.getLogger(IUtils.class);

	ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.setVisibility(PropertyAccessor.ALL, Visibility.NONE)
			.setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	static HttpServletRequest asHttp(ServletRequest req) {
		return (HttpServletRequest) req;
	}

	static HttpServletResponse asHttp(ServletResponse res) {
		return (HttpServletResponse) res;
	}

	static Optional<String> getHeader(HttpServletRequest req, String key) {
		return Optional.ofNullable(req.getHeader(key));
	}

	/**
	 * Check if an object is null
	 * @param object
	 * @return
	 */
	static boolean isNull(Object object) {
		if (null == object)
			return true;
		return false;
	}

	/**
	 * Method check if object is null or empty
	 * @param object
	 * @return
	 */
	static boolean isNullOrEmpty(String object) {
		if (!isNull(object) && !object.trim().isEmpty()) {
			return false;
		}
		return true;
	}

	static Class<?> loadClass(String cls) {
		if (!isNull(cls)) {
			try {
				return Class.forName(cls);
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		return null;
	}

	/**
	 * Method tried to load Class specified in param
	 * @param cls fully qualified name of class
	 * @return return null if not found
	 */
	static Class<?> getClass(String cls) {
		Class<?> entity = null;
		if (!isNullOrEmpty(cls)) {
			try {
				entity = Class.forName(cls);
			} catch (ClassNotFoundException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return entity;
	}

	/**
	 * Method to encode input string
	 * @param input
	 * @return
	 */
	static String encrypt(String input) {
		DefaultPasswordService service = new DefaultPasswordService();
		return service.encryptPassword(input);
		// return BCrypt.hashpw(input, BCrypt.gensalt());
	}

	/**
	 * Check that a plaintext password matches a previously hashed one
	 * @param hashed previously-hashed string
	 * @param plain the plaintext string to verify
	 * @return true if the passwords match, false otherwise
	 */
	static boolean matchWithEncrypted(String hashed, String plain) {
		DefaultPasswordService service = new DefaultPasswordService();
		return service.passwordsMatch(plain, hashed);
		// return BCrypt.checkpw(plain, hashed);
	}

	/**
	 * Method to convert json object into specified type object
	 * @param val
	 * @param cls
	 * @return
	 */
	static <T> T getObjectFromValue(String val, Class<T> cls) {
		T res = null;
		if (!isNull(val)) {
			try {
				res = OBJECT_MAPPER.readValue(val, cls);
			} catch (Exception e) {
				// ignore it
				logger.warn(e.getMessage(), e);
			}
		}
		return res;
	}

	/**
	 * Method to get {@code User} from {@code HttpServletRequest}
	 * @param request {@code HttpServletRequest}
	 * @return {@code User}
	 */
	static String getUserFromRequest(HttpServletRequest request) {
		String user = IConsts.ADMIN;
		if (!isNull(request) && !isNull(request.getUserPrincipal())) {
			user = request.getUserPrincipal().getName();
		} else {
			try {
				if (!isNull(SecurityUtils.getSubject())) {
					user = SecurityUtils.getSubject().getPrincipal().toString();
				}
			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
		return user;
	}

	/**
	 * Method returns list of string from input
	 * @param input
	 * @param delim
	 * @return empty List if input is null or empty
	 */
	static List<String> getListFromString(String input, String delim) {
		List<String> lst = new ArrayList<>();
		delim = ((IUtils.isNullOrEmpty(delim)) ? IConsts.DELIM_COMMA : delim);
		if (!isNullOrEmpty(input)) {
			StringTokenizer token = new StringTokenizer(input, delim);
			while (token.hasMoreTokens()) {
				String val = token.nextToken();
				if (!isNullOrEmpty(val)) {
					lst.add(val.trim());
				}
			}
		}
		return lst;
	}

	/**
	 * Method to create new Entity object form json
	 * @param <T>
	 * @param service
	 * @param user
	 * @return
	 */
	static <T> T createEntity(ObjectNode service, String user, Class<T> cls) {
		T object = null;
		if (!isNull(service) && !service.isEmpty(null)) {
			object = getObjectFromValue(service.toString(), cls);
			if (!isNull(object) && object instanceof Entity) {
				Entity ent = (Entity) object;
				if (isNullOrEmpty(ent.getCreatedBy())) {
					ent.setCreatedBy(user);
				}
				ent.setUpdatedBy(user);
				ent.setUpdatedAt(new Date());
			}
		}
		logger.debug("Entity: " + object);
		return object;
	}

	/**
	 * Method to return String array from JSONArray string
	 * @param input string like ["a","b"]
	 * @return empty List if input is null or empty
	 */
	static String[] getArrayFromJsonString(String input) {
		List<String> lst = new ArrayList<>();
		if (!isNullOrEmpty(input)) {
			try {
				JSONArray jarr = new JSONArray(input);
				for (int i = 0; i < jarr.length(); i++) {
					lst.add(jarr.get(i).toString());
				}
			} catch (Throwable th) {
				logger.error(th.getMessage(), th);
			}
		}
		return lst.toArray(new String[lst.size()]);
	}

	/**
	 * Method to generate failure response
	 * @param ex
	 * @return
	 */
	static Object getFailedResponse(Throwable ex) {
		return getFailedResponse(ex.getMessage());
	}

	/**
	 * Method to generate failure response
	 * @param ex
	 * @return
	 */
	static Object getFailedResponse(String msg) {
		JSONObject obj = new JSONObject();
		try {
			obj.put(IConsts.STATUS, IConsts.FAILED);
			obj.put(IConsts.CAUSE, msg);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return getResultObject(obj);
	}

	/**
	 * Method create {@code java.util.Date} object based on parameters
	 * @param date {@code Date} in String format
	 * @param locale {@code Locale} in string
	 * @param format {@code String} format pattern to parse from
	 * @return {@code java.util.Date} object or null
	 */
	static Date parseDate(String date, String locale, String format) {
		Date dt = null;
		if (!isNullOrEmpty(date)) {
			Locale loc = null;
			if (!isNullOrEmpty(locale)) {
				loc = new Locale(locale);
			}
			try {
				if (isNullOrEmpty(format)) {
					format = IConsts.DEF_DATE_FORMAT;
				}
				dt = DateUtils.parseDate(date, loc, format);
			} catch (ParseException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return dt;
	}

	/**
	 * Method to check if string is a valid long date, if yes returns formated
	 * date string. otherwise returns input string without any change.
	 * @param longDate date as long string value
	 * @param format date format pattern
	 * @param locale locale for getting time formated string.
	 * @return formated date string or input
	 */
	static String getFormatedDateFromLongString(String longDate, String format,
			String locale) {
		String res = null;
		if (!isNullOrEmpty(longDate) && !isNullOrEmpty(format)) {
			try {
				Locale loc = null;
				if (!isNullOrEmpty(locale)) {
					loc = Locale.forLanguageTag(locale);
				}
				Calendar cal = null;
				if (!isNull(loc)) {
					cal = Calendar.getInstance(loc);
				} else {
					cal = Calendar.getInstance();
				}
				cal.setTimeInMillis(Long.valueOf(longDate));
				Date dt = cal.getTime();
				DateFormat df = new SimpleDateFormat(format);
				res = df.format(dt);
			} catch (Throwable th) {
				logger.error("Invalid date in long (" + longDate
						+ ") or invalid date format (" + format + ")");
				// Failed to parse of translate string into date
				// Return input string
				res = longDate;
			}
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	static <T> T getFileAttribute(File file, String attKey) {
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(),
					BasicFileAttributes.class);
			switch (attKey) {
			case "createdAt":
				return (T) attr.creationTime();
			case "updatedAt":
				return (T) attr.lastModifiedTime();
			default:
				logger.error("Undefined attribute: " + attKey);
				break;
			}
		} catch (Throwable th) {
			logger.error(th.getMessage(), th);
		}
		return null;
	}

	/**
	 * Method to return jcr query path from absolute path
	 * @param path
	 * @return
	 */
	static String getJcrPath(String path) {
		String jcrPath = "/jcr:root";
		if (!isNullOrEmpty(path) && path.startsWith("/")) {
			jcrPath += path;
		}
		return jcrPath;
	}

	/**
	 * Method to verify and return jcr query columns
	 * @param cols
	 * @return
	 */
	static String getJcrColumns(String cols) {
		String jcrCols = ".";
		if (!isNullOrEmpty(cols)) {
			jcrCols = cols;
		}
		return jcrCols;
	}

	/**
	 * Method check if node is JSONObject then return string.
	 * @param node
	 * @return
	 */
	static Object getResultObject(Object node) {
		Object res = null;
		if (!IUtils.isNull(node)) {
			if (node instanceof JSONObject) {
				res = node.toString();
			} else {
				res = node;
			}
		}
		return res;
	}

	/**
	 * Method to send multipart post request for file uploading
	 * @param url
	 * @param params {@code Map<String, Object>}
	 * @param files {@code Map<String, File>}
	 * @return String response <b>if called /api/v1/action/upload service it
	 *         will return full file path on server to use in mail
	 *         attachments</b>
	 * @throws Exception
	 */
	static Object sendMultiPartPostReq(String url, Map<String, Object> params,
			Map<String, File> files) throws Exception {
		Assert.assertNotNull("Rest multipart post request url must not be null", url);
		Object res = null;
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(url);
			MultipartEntityBuilder fEntity = MultipartEntityBuilder.create();
			// Add additional params
			if (!isNull(params)) {
				params.keySet().forEach((key) -> {
					fEntity.addTextBody(key, getStringFromValue(params.get(key)));
				});
			}
			// Add all multipart data
			if (!isNull(files)) {
				files.keySet().forEach((key) -> {
					fEntity.addBinaryBody(key, files.get(key));
				});
			}

			post.setEntity(fEntity.build());
			System.out.println("executing request " + post.getRequestLine());
			HttpResponse response = client.execute(post);
			if (!isNull(response)) {
				res = EntityUtils.toString(response.getEntity());
			}
		} catch (Throwable e) {
			throw new Exception(e.getMessage(), e);
		}
		return res;
	}

	/**
	 * Method to call a rest service using POST method url and params
	 * @param url fully qualified url for rest service
	 * @param params {@code Map<String, Object>}
	 * @return {@code HttpResponse} as string
	 * @throws Exception
	 */
	static Object sendPostReq(String url, Map<String, Object> params) throws Exception {
		return sendPostReq(url, null, params);
	}

	/**
	 * Method to call a rest service using POST method url and params
	 * @param url fully qualified url for rest service
	 * @param headers {@code Map<String, String>}
	 * @param params {@code Map<String, Object>}
	 * @return {@code HttpResponse} as string
	 * @throws Exception
	 */
	static Object sendPostReq(String url, Map<String, String> headers,
			Map<String, Object> params) throws Exception {
		Assert.assertNotNull("Rest post request url must not be null", url);
		String input = getStringFromValue(params);
		return sendPostJsonDataReq(url, headers, input);
	}

	/**
	 * Method to call a rest service using POST method url and post data
	 * @param url fully qualified url for rest service
	 * @param headers {@code Map<String, String>}
	 * @param input post data in string format
	 * @return {@code HttpResponse} as string
	 * @throws Exception
	 */
	static Object sendPostJsonDataReq(String url, Map<String, String> headers,
			String input) throws Exception {

		Assert.assertNotNull("Rest post request url must not be null", url);
		Object res = null;
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(url);
			if (!isNull(headers)) {
				headers.keySet().forEach((key) -> {
					post.addHeader(key, headers.get(key));
				});
			} else {
				post.setHeader(IConsts.CONT_TYPE, IConsts.CONT_TYPE_JSON);
			}
			if (!isNull(input)) {
				logger.info("Params: " + input);
				StringEntity entity = new StringEntity(input);
				post.setEntity(entity);
			}
			HttpResponse response = client.execute(post);
			if (!isNull(response)) {
				res = EntityUtils.toString(response.getEntity());
			}
		} catch (Throwable e) {
			logger.info("Exception occured while hitting url : " + url + " "
					+ e.getMessage());
		}
		logger.info("Response after hitting url : " + url + " " + res);
		return res;
	}

	/**
	 * Method to call a rest service using GET method url and params
	 * @param url fully qualified url for rest service
	 * @param headers {@code Map<String, String>}
	 * @param params {@code Map<String, Object>}
	 * @return {@code HttpResponse} as string
	 * @throws Exception
	 */
	static String sendGetRestReq(String url, Map<String, String> headers,
			Map<String, Object> params) throws Exception {
		Assert.assertNotNull("Rest get request url must not be null", url);
		String res = null;
		try {
			HttpClient client = HttpClientBuilder.create().build();
			String uri = addParamsInUrl(url, params);
			logger.info("Get call for: " + uri);
			// add params if any
			HttpGet get = new HttpGet(uri);
			if (!isNull(headers)) {
				headers.keySet().forEach((key) -> {
					get.addHeader(key, headers.get(key));
				});
			}
			HttpResponse response = client.execute(get);
			if (!isNull(response)) {
				res = EntityUtils.toString(response.getEntity());
			}
		} catch (Throwable e) {
			logger.info("Exception occured while hitting url : " + url + " "
					+ e.getMessage());
		}
		logger.info("Response after hitting url : " + url + " " + res);
		return res;
	}

	/**
	 * Method add params into url for get request by using {@code URIBuilder}
	 * @param url fully qualified url
	 * @param params {@code Map<String, Object>}
	 * @return
	 */
	static String addParamsInUrl(String url, Map<String, Object> params) {
		String res = url;
		if (!isNull(params) && params.size() > 0) {
			try {
				List<NameValuePair> nvps = getNameValuePairList(params);
				res = new URIBuilder(url).addParameters(nvps).build().toString();
			} catch (Throwable e) {
				// ignore it
			}
		}
		return res;
	}

	/**
	 * Method returns {@code NameValuePair} list from params map. Map values
	 * must be in serialized form to convert into json
	 * @param params {@code Map<String, Object>}
	 * @return empty list if params are null or empty
	 * @throws Exception
	 */
	static List<NameValuePair> getNameValuePairList(Map<String, Object> params)
			throws Exception {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		if (!isNull(params) && !params.isEmpty()) {
			try {
				params.keySet().forEach((key) -> {
					String val = getStringFromValue(params.get(key));
					if (!isNullOrEmpty(val)) {
						nvps.add(new BasicNameValuePair(key, val));
					}
				});
			} catch (Throwable e) {
				throw new Exception(e.getMessage(), e);
			}
		}
		return nvps;
	}

	/**
	 * Method returns string for {@code Object} type
	 * {@code String, Number, Date} or create json for other type using
	 * {@code ObjectMapper}
	 * @param val {@code Object} instance
	 * @return {@code String}
	 */
	static String getStringFromValue(Object val) {
		String res = null;
		if (!isNull(val)) {
			if (val instanceof String || val instanceof Number || val instanceof Date) {
				res = val.toString();
			} else if (val instanceof JSONArray) {
				JSONArray jarr = (JSONArray) val;
				res = "[";
				for (int i = 0; i < jarr.length(); i++) {
					res += (i > 0 ? ", " : "") + getStringFromValue(jarr.opt(i));
				}
				res += "]";
			} else if (val instanceof JSONObject) {
				res = val.toString();
			} else if (val instanceof Map) {
				res = new JSONObject((Map<?, ?>) val).toString();
			} else if (val instanceof List) {
				res = getStringFromValue(new JSONArray((List<?>) val));
			} else {
				try {
					res = OBJECT_MAPPER.writeValueAsString(val);
				} catch (Exception e) {
					// ignore it
				}
			}
		}
		return res;
	}

	/**
	 * Method to read a file in specified json object class
	 * @param file
	 * @return
	 */
	static String readStringFromFile(File file) {
		if (!isNull(file)) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();
				while (line != null) {
					sb.append(line);
					line = br.readLine();
				}
				br.close();
				return sb.toString();
			} catch (Exception e) {
				logger.error(e.getMessage());
			} finally {
				if (!isNull(br))
					try {
						br.close();
					} catch (IOException e) {
					}
			}
		}
		return null;
	}

	/**
	 * Method to create map from json object
	 * @param json
	 * @return null or key value pair of json object
	 */
	static Map<String, Object> getMapFromJson(JSONObject json) {
		Map<String, Object> map = null;
		if (!isNull(json) && json.length() > 0) {
			map = new HashMap<>();
			JSONArray keys = json.names();
			for (int i = 0; i < keys.length(); i++) {
				String key = keys.optString(i);
				map.put(key, json.opt(key));
			}
		}
		return map;
	}

	/**
	 * Method to convert oakfilenode map from multipart files
	 * @param files
	 * @param path
	 * @param nodePath
	 * @return
	 * @throws IOException
	 */
	static Map<String, OakFileNode> getFileNodes(MultipartFile[] files, String path,
			String nodePath) throws IOException {
		Map<String, OakFileNode> map = null;
		if (!isNull(files) && files.length > 0) {
			map = new HashMap<>();
			for (int i = 0; i < files.length; i++) {
				String name = files[i].getOriginalFilename();
				File dest = new File(path + File.separator + name);
				// Create file and file path if not exists
				if (!isNull(dest) && !isNull(dest.getParentFile())
						&& !dest.getParentFile().exists()) {
					dest.getParentFile().mkdirs();
					dest.createNewFile();
				}
				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(dest));
				bos.write(files[i].getBytes());
				bos.close();
				if (!IUtils.isNullOrEmpty(nodePath)) {
					nodePath += "/" + name;
				}
				OakFileNode fileNode = OakFileNode.createNode(nodePath, dest);
				InputStream is = null;
				fileNode.setData(is);
				map.put(name, fileNode);
			}
		}
		return map;
	}

	/**
	 * Method create a temp storage path
	 * @param user
	 * @param tmpDir
	 * @return
	 */
	static String getTempFileSavePath(String user, String tmpDir) {
		String path = tmpDir;
		if (!isNull(user)) {
			path += File.separator + user;
		}
		path += File.separator + System.currentTimeMillis();
		return path;
	}

	/**
	 * Method remove the dir from file system
	 * @param tmpPath
	 */
	static void deleteFiles(String tmpPath) throws Exception {
		if (!isNullOrEmpty(tmpPath)) {
			File file = new File(tmpPath);
			if (!isNull(file) && file.exists()) {
				if (file.isDirectory() && !isNull(file.listFiles())) {
					File[] lst = file.listFiles();
					for (int i = 0; i < lst.length; i++) {
						deleteFiles(lst[i].getAbsolutePath());
					}
				}
				file.delete();
			}
		}
	}

	/**
	 * Method to convert stream into string
	 * @param fis
	 * @return
	 * @throws IOException
	 */
	static String convertStreamToString(InputStream fis) {
		StringBuilder sb = new StringBuilder();
		Reader r = null;
		try {
			r = new InputStreamReader(fis, IConsts.DEF_ENCODING); // or whatever
																	// encoding
			char[] buf = new char[1024];
			int amt = r.read(buf);
			while (amt > 0) {
				sb.append(buf, 0, amt);
				amt = r.read(buf);
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			try {
				r.close();
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}

	/**
	 * Method convert string into stream
	 * @param input
	 * @return
	 */
	static InputStream convertStringToStream(String input) {
		try {
			return new ByteArrayInputStream(input.getBytes(IConsts.DEF_ENCODING));
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Method to escape \\ in string
	 * @param input
	 * @return
	 */
	static String escapeStr(String input) {
		return input.replace("\\", "\\\\");
	}

	/**
	 * Method to set file stream in file node object
	 * @param node
	 */
	static void updateFileStream(OakFileNode node) {
		if (!isNull(node) && isNull(node.getData())) {
			if (!isNull(node.getPath())) {
				File file = new File(node.getPath());
				if (!isNull(file) && file.exists()) {
					try {
						node.setData(new FileInputStream(file));
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		}
	}

	/**
	 * Method to create request map for jcr node create request
	 * @param nodePath
	 * @param oakNode
	 * @param nodeName
	 * @return
	 */
	static Map<String, Object> getParamMap(String nodePath, JSONObject oakNode,
			String nodeName) {
		Map<String, Object> map = new HashMap<>();
		map.put("parentPath", nodePath);
		map.put("json", oakNode.toString());
		if (!isNullOrEmpty(nodeName)) {
			map.put("nodeName", nodeName);
		}
		return map;
	}

	/**
	 * Create param map for oak file node objects
	 * @param parentPath
	 * @param node
	 * @param nodeName
	 * @return
	 */
	static Map<String, Object> getParamMap(String parentPath, OakFileNode node,
			String nodeName) {
		Map<String, Object> map = new HashMap<>();
		map.put("parentPath", parentPath);
		map.put("json", node.toString());
		map.put("cls", OakFileNode.class.getName());
		if (!isNullOrEmpty(nodeName)) {
			map.put("nodeName", nodeName);
		}
		return map;
	}

	/**
	 * find subscription id from machine id
	 * @param machineId
	 * @return
	 */
	static String extractSubscriptionId(String machineId) {
		if (!isNullOrEmpty(machineId)) {
			List<String> list = getListFromString(machineId, ":");
			if (!isNull(list) && list.size() > 1) {
				return list.get(1);
			}
		}
		return machineId;
	}

	/**
	 * Method to send rest request using RestTemplate
	 * @param template
	 * @param url
	 * @param request
	 * @param cls
	 * @param params
	 * @param cntType
	 * @return
	 */
	static <T> T sendPostRestRequest(RestTemplate template, String url, Object request,
			Class<T> cls, Map<String, Object> params, MediaType cntType) {
		T res = null;
		logger.info("Url: " + url);
		if (!isNull(params) && !params.isEmpty()) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(cntType);
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			params.entrySet().forEach(entry -> {
				map.add(entry.getKey(), entry.getValue());
			});
			ResponseEntity<T> result = template.postForEntity(url,
					new HttpEntity<>(map, headers), cls);
			res = result.getBody();
		} else {
			res = template.postForObject(url, request, cls);
		}
		logger.info("Response: " + res);
		return res;
	}

	/**
	 * Method to send rest request using RestTemplate
	 * @param rest
	 * @param url
	 * @param params
	 * @param cls
	 * @return
	 */
	static <T> T sendGetRestRequest(RestTemplate template, String url,
			Map<String, Object> params, Class<T> cls) {
		url = addParamsInUrl(url, params);
		logger.info("Rest Call: " + url);
		T res = template.getForObject(url, cls);
		logger.info("Response: " + res);
		return res;
	}

	/**
	 * Method to send put rest request using RestTemplate
	 * @param template
	 * @param url
	 * @param headers
	 * @param params
	 * @param entity
	 * @param cls
	 * @return
	 */
	static <T> T sendPutRestRequest(RestTemplate template, String url,
			Map<String, String> headers, Map<String, Object> params, T entity,
			Class<T> cls) {
		return sendRestRequest(template, url, HttpMethod.PUT, headers, params, entity,
				cls);
	}

	/**
	 * Method to call exchange request api format with header and params
	 * @param template
	 * @param url
	 * @param method
	 * @param headers
	 * @param params
	 * @param entity
	 * @param cls
	 * @return
	 */
	static <T> T sendRestRequest(RestTemplate template, String url, HttpMethod method,
			Map<String, String> headers, Map<String, Object> params, T entity,
			Class<T> cls) {
		HttpHeaders httpHdr = getHttpHeaders(headers);
		logger.info("Rest Call: " + url);
		HttpEntity<T> request = null;
		if (!isNull(httpHdr) || !isNull(entity)) {
			request = new HttpEntity<T>(entity, httpHdr);
		}
		ResponseEntity<T> res = template.exchange(url, method, request, cls, params);
		logger.info("Response: " + res);
		return res.getBody();
	}

	/**
	 * Method to get {@link HttpHeaders} object from headers map
	 * @param headers
	 * @return
	 */
	static HttpHeaders getHttpHeaders(Map<String, String> headers) {
		HttpHeaders httpHdr = new HttpHeaders();
		// Add headers
		if (!isNull(headers) && !headers.isEmpty()) {
			headers.entrySet().forEach(item -> {
				httpHdr.add(item.getKey(), item.getValue());
			});
		}
		return httpHdr;
	}

	/**
	 * Method creates a map from list of key value string or objects
	 * @param keyValPairs array of string, object, string, object
	 * @return
	 */
	static Map<String, Object> getRestParamMap(Object... keyValPairs) {
		Map<String, Object> map = new HashMap<>();
		if (!isNull(keyValPairs) && (keyValPairs.length % 2) == 0) {
			for (int i = 0; (i + 1) < keyValPairs.length;) {
				String key = (String) keyValPairs[i++];
				map.put(key, keyValPairs[i++]);
			}
		}
		return map;
	}
}