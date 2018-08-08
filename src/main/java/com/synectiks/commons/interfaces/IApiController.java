/**
 * 
 */
package com.synectiks.commons.interfaces;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Rajesh
 */
public interface IApiController {

	String API_PATH = "";
	String AUTH_API = API_PATH + "/auth";
	String SEC_API = API_PATH + "/security";
	String PUB_API = SEC_API + "/public";
	String SSM_API = API_PATH + "/ssm";
	String PLC_API = API_PATH + "/policy";
	
	String URL_ACTION = "/action";
	String URL_ACTIVITY = "/activity";
	String URL_MIGRATION = "/migration";
	String URL_OAKREPO = "/oakRepo";
	String URL_PAYMENT = "/payment";
	String URL_PERMISSION = "/permissions";
	String URL_RATING = "/rating";
	String URL_RECOM = "/recom";
	String URL_ROLES = "/roles";
	String URL_RESOURCE = "/resource";
	String URL_SEARCH = "/search";
	String URL_SERVICE = "/service";
	String URL_SSM = "/states";
	String URL_SUBSCRIPTION = "/subscription";
	String URL_USER = "/users";
	// String URL_ = "";

	/**
	 * List all the entities from repository
	 * @param request
	 * @return
	 */
	ResponseEntity<Object> findAll(HttpServletRequest request);

	/**
	 * API to load entity by id
	 * @param id
	 * @return
	 */
	ResponseEntity<Object> findById(String id);

	/**
	 * API to load entity by id
	 * @param id
	 * @return
	 */
	ResponseEntity<Object> deleteById(String id);

	/**
	 * Api to create a new entity
	 * @param entity
	 * @param request
	 * @return
	 */
	ResponseEntity<Object> create(ObjectNode entity, HttpServletRequest request);

	/**
	 * API to update an entity
	 * @param entity
	 * @param request
	 * @return
	 */
	ResponseEntity<Object> update(ObjectNode entity, HttpServletRequest request);

	/**
	 * API to delete and entity
	 * @param entity
	 * @return
	 */
	ResponseEntity<Object> delete(ObjectNode entity);
}
