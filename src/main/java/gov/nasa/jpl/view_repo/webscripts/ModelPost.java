/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * 
 * U.S. Government sponsorship acknowledged.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.ModelLoadActionExecuter;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Descriptor file:
 * /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts
 * /gov/nasa/jpl/javawebscripts/model.post.desc.xml
 * 
 * NOTE: Transactions are independently managed in this Jave webscript, so make
 * sure that the descriptor file has transactions set to none
 * 
 * @author cinyoung
 * 
 *         TODO Need merge? and force? similar to View?
 * 
 */
public class ModelPost extends AbstractJavaWebScript {
    public ModelPost() {
        super();
    }
    
    public ModelPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


//    private EmsScriptNode projectNode = null;
    // when run in background as an action, this needs to be false
    private boolean runWithoutTransactions = false;
//    protected String projectId;

    private final String ELEMENTS = "elements";

    /**
     * JSONObject of element hierarchy 
     * { 
     *  elementId: [childElementId, ...], 
     *  ...
     * },
     */
    private JSONObject elementHierarchyJson;

    /**
     * JSONObject of the relationships
     * "relationshipElements": {
     *      relationshipElementId: {
     *          "source": sourceElementId,
     *          "target": targetElementId
     *      },
     *      ... 
     * },
     * "propertyTypes": { 
     *      propertyElementId: typeElementId, //for Property to the property type
     *      ... 
     * },
     * "elementValues": { 
     *      propertyElementId: [elementId], //for property with ElementValue as value types, the value is a noderef
     *      ...
     * }
     */
    protected JSONObject relationshipsJson;

    protected Set<String> newElements;

    protected SiteInfo siteInfo;
    
    /**
     * Create or update the model as necessary based on the request
     * 
     * @param content
     *            JSONObject used to create/update the model
     * @param status
     *            Status to be updated
     * @throws JSONException
     *             Parse error
     */
    public void createOrUpdateModel(Object content, Status status, EmsScriptNode projectNode)
            throws Exception {
        Date now = new Date();
        log(LogLevel.INFO, "Starting createOrUpdateModel: " + now);
        long start = System.currentTimeMillis(), end, total = 0;

        clearCaches();

        JSONObject postJson = (JSONObject) content;

        boolean singleElement = !postJson.has(ELEMENTS);
        
        // check whether single JSONObject to create or an array
        if (singleElement) {
            updateOrCreateElement(postJson, projectNode, false);
        } else {
            // create the element map and hierarchies
            if (buildElementMap(postJson.getJSONArray(ELEMENTS), projectNode)) {
                // start building up elements from the root elements
                for (String rootElement : rootElements) {
                    log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
                    if (!rootElement.equals((String) projectNode.getProperty(Acm.CM_NAME))) {
                        
                        EmsScriptNode owner = null;

                        UserTransaction trx;
                        trx = services.getTransactionService().getNonPropagatingUserTransaction();
                        try {
                            trx.begin();
                            owner = getOwner(rootElement, projectNode, true);
                            trx.commit();
                        } catch (Throwable e) {
                            try {
                                trx.rollback();
                                log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                                e.printStackTrace();
                            } catch (Throwable ee) {
                                log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                                ee.printStackTrace();
                            }
                        }
                        
                        if (owner != null) {
                            updateOrCreateElement(elementMap.get(rootElement), owner, false);
                        }
                    }
                } // end for (String rootElement: rootElements) {
            } // end if (buildElementMap(postJson.getJSONArray(ELEMENTS))) {
        }

        // handle the relationships
        updateOrCreateAllRelationships(relationshipsJson);
        
        updateNodeReferences(singleElement, postJson, projectNode );
        
        now = new Date();
        end = System.currentTimeMillis();
        total = end -start;
        log(LogLevel.INFO, "createOrUpdateModel completed" + now + " : " +  total + "ms\n");
    }
    
    protected void updateNodeReferences(boolean singleElement, JSONObject postJson, EmsScriptNode projectNode ) throws Exception {
        if ( singleElement ) {
            updateOrCreateElement(postJson, projectNode, true);
        }
        for (String rootElement : rootElements) {
            log(LogLevel.INFO, "ROOT ELEMENT FOUND: " + rootElement);
            if (!rootElement.equals((String) projectNode.getProperty(Acm.CM_NAME))) {
                EmsScriptNode owner = getOwner( rootElement, projectNode, false );
                
                try {
                    updateOrCreateElement(elementMap.get(rootElement), owner, true);
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        } // end for (String rootElement: rootElements) {
    }

    protected EmsScriptNode getOwner(String elementId, EmsScriptNode projectNode, boolean createOwnerPkgIfNotFound) {
        JSONObject element = elementMap.get(elementId);
        if ( element == null ) {
            log(LogLevel.ERROR, "Trying to get owner of null element!", HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        String ownerName = null;
        if (element.has(Acm.JSON_OWNER)) {
            try{
                ownerName = element.getString(Acm.JSON_OWNER);
            } catch ( JSONException e ) {
                e.printStackTrace();
            }
        }
        
        // get the owner so we can create node inside owner
        // DirectedRelationships can be sent with no owners, so, if not specified look for its existing owner
        EmsScriptNode owner = null;
        EmsScriptNode reifiedPkg = null;
        if (ownerName == null || ownerName.equals("null")) {
            EmsScriptNode elementNode = findScriptNodeById(elementId);
            if (elementNode == null || !elementNode.exists()) {
                owner = projectNode;
            } else {
                owner = elementNode.getParent();
            }
        } else {
       		boolean isOwnerParent = false;
            owner = findScriptNodeById(ownerName);
            if (owner == null || !owner.exists()) {
                log(LogLevel.WARNING, "Could not find owner with name: " + ownerName + " putting " + elementId + " into project", HttpServletResponse.SC_BAD_REQUEST);
                owner = projectNode;
                isOwnerParent = true;
            }
            // really want to add pkg as owner
            reifiedPkg = findScriptNodeById(ownerName + "_pkg");
            if (reifiedPkg == null || !reifiedPkg.exists()) {
                if ( createOwnerPkgIfNotFound) {
                    reifiedPkg = getOrCreateReifiedNode(owner, ownerName, isOwnerParent);
                } else {
                    log(LogLevel.WARNING, "Could not find owner package: " + ownerName, HttpServletResponse.SC_NOT_FOUND);
                }
            }
            owner = reifiedPkg;
        }
        log( LogLevel.INFO, "\tgetOwner(" + elementId + "): json element=("
                            + element + "), ownerName=" + ownerName
                            + ", reifiedPkg=(" + reifiedPkg + ", projectNode=("
                            + projectNode + "), returning owner=" + owner );
        return owner;
    }
    
    protected void updateOrCreateAllRelationships(JSONObject jsonObject) throws JSONException {
        updateOrCreateRelationships(jsonObject, "relationshipElements");
        updateOrCreateRelationships(jsonObject, "propertyTypes");
        updateOrCreateRelationships(jsonObject, "elementValues");
        updateOrCreateRelationships(jsonObject, "annotatedElements");
    }

    /**
     * Update or create relationships
     * 
     * @param jsonObject
     *            Input data to generate relationships from
     * @param key
     *            The relationship type (e.g., relationshipElements,
     *            projectTypes, or elementValues)
     * @throws JSONException
     */
    protected void updateOrCreateRelationships(JSONObject jsonObject, String key)
            throws JSONException {
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateRelationships" + key + ": ");
        if (runWithoutTransactions) {
            updateOrCreateTransactionableRelationships(jsonObject, key);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                log(LogLevel.INFO, "updateOrCreateRelationships: beginning transaction {");
                updateOrCreateTransactionableRelationships(jsonObject, key);
                log(LogLevel.INFO, "} updateOrCreateRelationships committing: " + key);
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                }
            }
        }
        end = System.currentTimeMillis();
        log(LogLevel.INFO, (end - start) + "ms");
    }
    
    protected void updateOrCreateTransactionableRelationships(JSONObject jsonObject, String key) throws JSONException {
        if (jsonObject.has(key)) {
            JSONObject object = jsonObject.getJSONObject(key);
            Iterator<?> ids = object.keys();
            while (ids.hasNext()) {
                String id = (String) ids.next();
                if (key.equals("relationshipElements")) {
                    updateOrCreateRelationship(object.getJSONObject(id), id);
                } else if (key.equals("propertyTypes")) {
                    updateOrCreatePropertyType(object.getString(id), id);
                } else if (key.equals("elementValues")) {
                    updateOrCreateElementValues(object.getJSONArray(id), id);
                } else if (key.equals("annotatedElements")) {
                    updateOrCreateAnnotatedElements(object.getJSONArray(id), id);
                }
            }
        }
    }    

    /**
     * Update or create annotated elements (multiple noderefs ordered in a list)
     * 
     * @param jsonArray
     *            Array of the IDs that house the values for the element
     * @param id
     *            The ID of the element to add the values to
     * @throws JSONException
     */
    protected void updateOrCreateAnnotatedElements(JSONArray jsonArray, String id)
            throws JSONException {
        EmsScriptNode source = findScriptNodeById(id);

        if (checkPermissions(source, PermissionService.WRITE)) {
            for (int ii = 0; ii < jsonArray.length(); ii++) {
                String targetId = jsonArray.getString(ii);
                EmsScriptNode target = findScriptNodeById(targetId);
                if (target != null) {
                    source.createOrUpdateAssociation(target, Acm.ACM_ANNOTATED_ELEMENTS, true);
                }
            }
        }
    }

    /**
     * TODO this may be outdated.  ElementValue is not longer a property.
     * 		these should be done for ValueSpecification types
     * 
     * Update or create element values (multiple noderefs ordered in a list)
     * 
     * @param jsonArray
     *            Array of the IDs that house the values for the element
     * @param id
     *            The ID of the element to add the values to
     * @throws JSONException
     */
    protected void updateOrCreateElementValues(JSONArray jsonArray, String id)
            throws JSONException {
        // create an array of the values to be added in as the elementValue
        // property
        ArrayList<NodeRef> values = new ArrayList<NodeRef>();
        for (int ii = 0; ii < jsonArray.length(); ii++) {
            String valueId = jsonArray.getString(ii);
            EmsScriptNode value = findScriptNodeById(valueId);
            if (value != null
                    && checkPermissions(value, PermissionService.WRITE)) {
                values.add(value.getNodeRef());
            } else {
                log(LogLevel.ERROR,
                        "could not find element value node with id " + valueId
                                + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        // only change if old list is different than new
        EmsScriptNode element = findScriptNodeById(id);
        if (element != null
                && checkPermissions(element, PermissionService.WRITE)) {
            @SuppressWarnings("unchecked")
            ArrayList<NodeRef> oldValues = (ArrayList<NodeRef>) element
                    .getProperty(Acm.ACM_ELEMENT_VALUE);
            if (!EmsScriptNode.checkIfListsEquivalent(values, oldValues)) {
                element.setProperty(Acm.ACM_ELEMENT_VALUE, values);
            }
        } else {
            log(LogLevel.ERROR, "could not find element node with id " + id
                    + "\n", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Update or create the property type association between an element and its
     * type
     * 
     * @param typeId
     *            ID of the type
     * @param id
     *            ID of the element
     */
    protected void updateOrCreatePropertyType(String typeId, String id) {
        EmsScriptNode property = findScriptNodeById(id);
        EmsScriptNode propertyType = findScriptNodeById(typeId);

        if (property != null && propertyType != null) {
            if (checkPermissions(property, PermissionService.WRITE)
                    && checkPermissions(propertyType, PermissionService.READ)) {
                property.createOrUpdateAssociation(propertyType, Acm.ACM_PROPERTY_TYPE);
            }
        } else {
            if (property == null) {
                log(LogLevel.ERROR, "could not find property node with id "
                        + id + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
            if (propertyType == null) {
                log(LogLevel.ERROR,
                        "could not find property type node with id " + typeId
                                + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    /**
     * Update or create the element relationship associations
     * 
     * @param jsonObject
     *            JSONObject that defines the source and target of the directed
     *            relationship
     * @param id
     *            Id of the directed relationship element
     * @throws JSONException
     */
    protected void updateOrCreateRelationship(JSONObject jsonObject, String id)
            throws JSONException {
        String sourceId = jsonObject.getString(Acm.JSON_SOURCE);
        String targetId = jsonObject.getString(Acm.JSON_TARGET);

        EmsScriptNode relationship = findScriptNodeById(id);
        EmsScriptNode source = findScriptNodeById(sourceId);
        EmsScriptNode target = findScriptNodeById(targetId);

        if (relationship != null && source != null && target != null) {
            if (checkPermissions(relationship, PermissionService.WRITE)
                    && checkPermissions(source, PermissionService.READ)
                    && checkPermissions(target, PermissionService.READ)) {
                relationship.createOrUpdateAssociation(source, Acm.ACM_SOURCE);
                relationship.createOrUpdateAssociation(target, Acm.ACM_TARGET);
            }
        } else {
            if (relationship == null) {
                log(LogLevel.ERROR, "could not find relationship node with id "
                        + id + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
            if (source == null) {
                log(LogLevel.ERROR, "could not find source node with id "
                        + sourceId + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
            if (target == null) {
                log(LogLevel.ERROR, "could not find target node with id "
                        + targetId + "\n", HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }


    Map<String, JSONObject> elementMap = new HashMap<String, JSONObject>();
    Set<String> rootElements = new HashSet<String>();

    /**
     * Builds up the element map and hierarchy and returns true if valid
     * @param jsonArray         Takes in the elements JSONArray
     * @return                  True if all elements and owners can be found with write permissions, false otherwise
     */
    protected boolean buildElementMap(JSONArray jsonArray, EmsScriptNode projectNode) throws JSONException {
        boolean isValid = true;

        if (runWithoutTransactions) {
            isValid =  buildTransactionableElementMap(jsonArray, projectNode);
        } else {
            UserTransaction trx;
            // building element map is a read-only transaction
            trx = services.getTransactionService().getNonPropagatingUserTransaction(true);
            try {
                trx.begin();
                log(LogLevel.INFO, "buildElementMap begin transaction {");
                isValid = buildTransactionableElementMap(jsonArray, projectNode);
                log(LogLevel.INFO, "} buildElementMap committing");
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                }
                isValid = false;
            }
        }
        
        return isValid;
    }

    protected boolean buildTransactionableElementMap(JSONArray jsonArray, EmsScriptNode projectNode) throws JSONException {
        boolean isValid = true;
        for (int ii = 0; ii < jsonArray.length(); ii++) {
            JSONObject elementJson = jsonArray.getJSONObject(ii);
            String sysmlId = null;
            try {
                sysmlId = elementJson.getString( Acm.JSON_ID );
            } catch ( JSONException e ) {
                // ignore
            }
            if ( sysmlId == null ) {
                log( LogLevel.ERROR, "No id in element json!",
                     HttpServletResponse.SC_NOT_FOUND );
                continue;
            }
            elementMap.put(sysmlId, elementJson);

            if (findScriptNodeById(sysmlId) == null) {
                newElements.add(sysmlId);
            }

            // create the hierarchy
            if (elementJson.has(Acm.JSON_OWNER)) {
                String ownerId = elementJson.getString(Acm.JSON_OWNER);
                // if owner is null, leave at project root level
                if (ownerId == null || ownerId.equals("null")) {
                    ownerId = (String) projectNode.getProperty(Acm.ACM_ID);
                    rootElements.add(sysmlId);
                }
                if (!elementHierarchyJson.has(ownerId)) {
                    elementHierarchyJson.put(ownerId, new JSONArray());
                }
                elementHierarchyJson.getJSONArray(ownerId).put(sysmlId);
            } else {
                // if no owners are specified, add directly to root elements
                rootElements.add(sysmlId);
            }
        }

        // lets iterate through elements
       for (String elementId: elementMap.keySet()) {
            if (!newElements.contains(elementId)) {
                EmsScriptNode element = findScriptNodeById(elementId);
                if (element == null) {
                    log(LogLevel.ERROR, "Could not find node with id: " + elementId, HttpServletResponse.SC_BAD_REQUEST);
                } else if (!checkPermissions(element, PermissionService.WRITE)) {
                		// do nothing, just log inside of checkPermissions
                }
            }
        }
        
       	if (isValid) {
    	   		isValid = fillRootElements();
       	}
        
        return isValid;
    }

    protected boolean fillRootElements() throws JSONException {
        Iterator<?> iter = elementHierarchyJson.keys();
        while (iter.hasNext()) {
            String ownerId = (String) iter.next();
            if (!elementMap.containsKey(ownerId)) {
                JSONArray hierarchy = elementHierarchyJson
                        .getJSONArray(ownerId);
                for (int ii = 0; ii < hierarchy.length(); ii++) {
                    rootElements.add(hierarchy.getString(ii));
                }
            }
        }
        
        for (String name: rootElements) {
        		EmsScriptNode rootElement = findScriptNodeById(name);
        		if (rootElement != null) {
	        		if (!checkPermissions(rootElement, PermissionService.WRITE)) {
	        			log(LogLevel.WARNING, "\tskipping as root element since no write permissions", HttpServletResponse.SC_BAD_REQUEST);
	        		}
        		}
        }
        return true;
    }

    /**
     * Update or create element with specified metadata
     * 
     * @param jsonObject
     *            Metadata to be added to element
     * @param key
     *            ID of element
     * @throws JSONException
     */
    protected void updateOrCreateElement(JSONObject elementJson,
                                         EmsScriptNode parent, boolean ingest) throws Exception {
		// check that parent is of folder type
		if (!services.getDictionaryService().isSubClass(parent.getQNameType(), ContentModel.TYPE_FOLDER)) {
			String name = (String) parent.getProperty(Acm.ACM_NAME);
			if (name == null) {
				name = (String) parent.getProperty(Acm.CM_NAME);
			}
			String id = (String) parent.getProperty(Acm.ACM_ID);
			if (id == null) {
				id = "not sysml type";
			}
			log(LogLevel.WARNING, "Node " + name + " is not of type folder, so cannot create children [id=" + id + "]");
			return;
		}
    	
        JSONArray children = new JSONArray();
        
        EmsScriptNode reifiedNode = null;
        
        if (runWithoutTransactions) {
            reifiedNode = updateOrCreateTransactionableElement(elementJson, parent, children, ingest);
        } else {
            UserTransaction trx;
            trx = services.getTransactionService().getNonPropagatingUserTransaction();
            try {
                trx.begin();
                log(LogLevel.INFO, "updateOrCreateElement begin transaction {");
                reifiedNode = updateOrCreateTransactionableElement(elementJson, parent, children, ingest);
                log(LogLevel.INFO, "} updateOrCreateElement end transaction");
                trx.commit();
            } catch (Throwable e) {
                try {
                    trx.rollback();
                    log(LogLevel.ERROR, "\t####### ERROR: Needed to rollback: " + e.getMessage());
                    e.printStackTrace();
                } catch (Throwable ee) {
                    log(LogLevel.ERROR, "\tRollback failed: " + ee.getMessage());
                    ee.printStackTrace();
                }
            }
        }

        // create the children elements
        if (reifiedNode != null && reifiedNode.exists()) {
            for (int ii = 0; ii < children.length(); ii++) {
                updateOrCreateElement(elementMap.get(children.getString(ii)),
                                      reifiedNode, ingest);
            }
        }
    }

    protected EmsScriptNode updateOrCreateTransactionableElement(JSONObject elementJson, EmsScriptNode parent, JSONArray children, boolean ingest) throws Exception {
        if (!elementJson.has(Acm.JSON_ID)) {
            return null;
        }
        String id = elementJson.getString(Acm.JSON_ID);
        long start = System.currentTimeMillis(), end;
        log(LogLevel.INFO, "updateOrCreateElement " + id);

        // TODO Need to permission check on new node creation
        // find node if exists, otherwise create
        EmsScriptNode node = findScriptNodeById( id );
        EmsScriptNode reifiedNode = null;
        if ( (node == null || !node.exists()) ) {// && newElements.contains( id ) ) {
            String type = null;
            String jsonType = elementJson.getString( Acm.JSON_TYPE );
            if ( jsonType != null ) type = Acm.getJSON2ACM().get( jsonType );
            if ( type == null || type.trim().isEmpty() ) {
                System.out.println( "PREFIX: type not found for " + jsonType );
                return null;
            } else {
                log( LogLevel.INFO, "\tcreating node" );
                try {
                    node = parent.createNode( id, type );
                    if ( node == null || !node.exists() ) throw new Exception( "createNode() failed." );
                    node.setProperty( Acm.CM_NAME, id );
                    node.setProperty( Acm.ACM_ID, id );
                } catch ( Exception e ) {
                    System.out.println( "Got exception in "
                                        + "updateOrCreateTransactionableElement(elementJson="
                                        + elementJson + ", parent=("
                                        + parent + "), children=(" + children
                                        + ")), calling parent.createNode(id=" + id + ", " + type + ")" );
                    throw e;
                }
            }
        } else {
            log(LogLevel.INFO, "\tmodifying node");
            try {
                if (node != null && node.exists() && !node.getParent().equals(parent)) {
                    node.move(parent);
                    EmsScriptNode pkgNode = findScriptNodeById(id + "_pkg");
                    if (pkgNode != null) {
                        pkgNode.move(parent);
                    }
                }
            } catch (Exception e) {
                log(LogLevel.WARNING, "could not find node information: " + id);
                e.printStackTrace();
            }
        }
        boolean nodeExists = node != null && node.exists(); 
        if (id != null && nodeExists ) {
            foundElements.put(id, node); // cache the found value
        }

        // update metadata
        if (ingest && nodeExists && checkPermissions(node, PermissionService.WRITE)) {
            log(LogLevel.INFO, "\tinserting metadata");
            node.ingestJSON(elementJson);
        }

        if (nodeExists && elementHierarchyJson.has(id)) {
            log(LogLevel.INFO, "\tcreating reified package");
            reifiedNode = getOrCreateReifiedNode(node, id, true); // TODO -- Is last argument correct?
            
            JSONArray array = elementHierarchyJson.getJSONArray(id);
            if ( array != null ) {
            	// GG: removed !ingest so children can have their metadata inserted
                //for (int ii = 0; !ingest && ii < array.length(); ii++) {
                for (int ii = 0; ii < array.length(); ii++) {
                    children.put(array.get(ii));
                }
            }
        }

        // add the relationships into our maps
        // REVIEW -- Should we skip this if the node or reified node does not exist?
        // REVIEW -- Should we throw an exception if the node or reified node does not exist?
        log(LogLevel.INFO, "\tfiltering relationships");
        JSONObject relations = EmsScriptNode.filterRelationsJSONObject(elementJson);
        String keys[] = { 
                "elementValues",
                "propertyTypes",
                "relationshipElements",
                "annotatedElements" };
        if ( !ingest ) for (String key : keys) {
            if (!relationshipsJson.has(key)) {
                relationshipsJson.put(key, new JSONObject());
            }
            if (relations.has(key)) {
                JSONObject json = relations.getJSONObject(key);
                Iterator<?> iter = json.keys();
                while (iter.hasNext()) {
                    String iterId = (String) iter.next();
                    relationshipsJson.getJSONObject(key).put(iterId,
                            json.get(iterId));
                }
            }
        }

        end = System.currentTimeMillis(); log(LogLevel.INFO, "\tTotal: " + (end-start) + " ms");
        return reifiedNode;
    }
    
    protected EmsScriptNode getOrCreateReifiedNode(EmsScriptNode node, String id, boolean useParent) {
        EmsScriptNode reifiedNode = null;
        if ( node == null || !node.exists() ) {
            log( LogLevel.ERROR,
                 "Trying to create reified node for missing node! id = " + id );
            return null;
        }
        EmsScriptNode parent;
        if (useParent) {
            parent= node.getParent();
        } else {
            parent = node;
        }
        if ( parent == null || !parent.exists() ) {
            log( LogLevel.ERROR,
                 "Trying to create reified node folder in missing parent folder for node " + node );
            return null;
        }

        if (checkPermissions(parent, PermissionService.WRITE)) {
            String pkgName = id + "_pkg";
            reifiedNode = findScriptNodeById(pkgName);
            if (reifiedNode == null || !reifiedNode.exists()) {
                reifiedNode = parent.createFolder(pkgName, Acm.ACM_ELEMENT_FOLDER);
                if (reifiedNode == null || !reifiedNode.exists()) {
                    log(LogLevel.ERROR, "\t failed to create reified node " + pkgName + " in " + parent.getProperty(Acm.CM_NAME));
                    return null;
                } else {
                    reifiedNode.setProperty(Acm.ACM_ID, pkgName);
                    reifiedNode.setProperty(Acm.CM_NAME, pkgName);
                    reifiedNode.setProperty(Acm.ACM_NAME, (String) node.getProperty(Acm.ACM_NAME));
                    log(LogLevel.INFO, "\tcreating " + pkgName + " in " + parent.getProperty(Acm.CM_NAME) + " : " + reifiedNode.getNodeRef().toString());
                }
            }
            if (checkPermissions(reifiedNode, PermissionService.WRITE)) {
                foundElements.put(pkgName, reifiedNode);
                // this reification containment association is more trouble than it's worth...
//                node.createOrUpdateChildAssociation(reifiedNode, Acm.ACM_REIFIED_CONTAINMENT);
            }
        }

        return reifiedNode;
    }

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
            Status status, Cache cache) {
        Map<String, Object> model = new HashMap<String, Object>();
        clearCaches();
        
//        Acm.ACM2JSON = null;
//        Acm.JSON2ACM = null;
        System.out.println("Acm.getJSON2ACM() = " + Acm.getJSON2ACM());
        System.out.println("Acm.getACM2JSON() = " + Acm.getACM2JSON());
        
        boolean runInBackground = checkArgEquals(req, "background", "true");

        ModelPost instance = new ModelPost(repository, services);
        
        if (validateRequest(req, status)) {
            try {
                if (runInBackground) {
                    instance.saveAndStartAction(req, status);
                    response.append("JSON uploaded, model load being processed in background.\n");
                    response.append("You will be notified via email when the model load has finished.\n");
                } else {
                    instance.createOrUpdateModel(req.parseContent(), status, getProjectNodeFromRequest(req));
                }
                appendResponseStatusInfo(instance);
            } catch (JSONException e) {
                log(LogLevel.ERROR, "JSON malformed\n", HttpServletResponse.SC_BAD_REQUEST);
                e.printStackTrace();
            } catch (Exception e) {
                log(LogLevel.ERROR, "Internal error stack trace:\n" + e.getLocalizedMessage() + "\n", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                e.printStackTrace();
            }
        }

        status.setCode(responseStatus.getCode());
        model.put("res", response.toString());
        return model;
    }

    protected void saveAndStartAction(WebScriptRequest req, Status status) throws Exception {
        //String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        //SiteInfo siteInfo = services.getSiteService().getSite(siteName);
        EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        
        String jobName = "Load Job " + projectId + ".json";
        EmsScriptNode jobNode = ActionUtil.getOrCreateJob(siteNode, jobName, "ems:Job", status, response);
        
        // write out the json
        ActionUtil.saveStringToFile(jobNode, "application/json", services, ((JSONObject)req.parseContent()).toString(4));
        
        EmsScriptNode projectNode = findScriptNodeById(projectId);
        // kick off the action
        ActionService actionService = services.getActionService();
        Action loadAction = actionService.createAction(ModelLoadActionExecuter.NAME);
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_ID, projectId);
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NAME, (String)projectNode.getProperty(Acm.ACM_NAME));
        loadAction.setParameterValue(ModelLoadActionExecuter.PARAM_PROJECT_NODE, projectNode);
        services.getActionService().executeAction(loadAction , jobNode.getNodeRef(), true, true);
    }
    
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null) {
            // TODO - move this to ViewModelPost - really non hierarchical post
            if (!checkRequestVariable(elementId, "elementid")) {
                return false;
            }
        } else {
            String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
            // Handling for project/{id}/elements
            if (!checkRequestVariable(siteName, SITE_NAME)) {
                return false;
            }

            siteInfo = services.getSiteService().getSite(siteName);
            if (!checkRequestVariable(siteInfo, "Site")) {
                return false;
            }
        }
        
        // check project
        EmsScriptNode projectNode = getProjectNodeFromRequest(req);
        if (projectNode == null) {
            log(LogLevel.ERROR, "Project not found.\n", HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        
        return true;
    }

    private EmsScriptNode getProjectNodeFromRequest(WebScriptRequest req) {
        EmsScriptNode projectNode = null;
        
        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null) {
            // projectNode should be the owner..., which should exist
            try {
                JSONObject postJson = (JSONObject) req.parseContent();
                JSONObject elementsJson = postJson.getJSONObject("elements");
                JSONObject elementJson = elementsJson.getJSONObject(elementId);
                projectNode = findScriptNodeById(elementJson.getString(Acm.JSON_OWNER));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
            String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
            EmsScriptNode siteNode = getSiteNode(siteName);
            projectNode = siteNode.childByNamePath("/Models/" + projectId);
            if (projectNode == null) {
            		// for backwards compatibility
            		projectNode = siteNode.childByNamePath("/ViewEditor/" + projectId);
            }
        }
        
        return projectNode;
    }
    
    public void setRunWithoutTransactions(boolean withoutTransactions) {
        runWithoutTransactions = withoutTransactions;           
    }
    
    @Override
    protected void clearCaches() {
        super.clearCaches();
        elementHierarchyJson = new JSONObject();
        relationshipsJson = new JSONObject();
        rootElements = new HashSet<String>();
        elementMap = new HashMap<String, JSONObject>();
        newElements = new HashSet<String>();
    }
}
