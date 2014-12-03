/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
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

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MmsWorkspaceDiffPost extends ModelPost {
	public MmsWorkspaceDiffPost() {
	    super();
	}
    
    public MmsWorkspaceDiffPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

	
    @Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// do nothing
		return false;
	}
	
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
	    MmsWorkspaceDiffPost instance = new MmsWorkspaceDiffPost(repository, services);
	    return instance.executeImplImpl( req, status, cache );
	}
	
	
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        printHeader( req );

		clearCaches();
		
		Map<String, Object> model = new HashMap<String, Object>();
        
		try {
			handleDiff((JSONObject)req.parseContent(), status, model);
		} catch (JSONException e) {
			log(LogLevel.ERROR, "JSON parse exception: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
			e.printStackTrace();
		} catch ( Exception e ) {
            log(LogLevel.ERROR, "Internal server error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
		
        status.setCode(responseStatus.getCode());
		model.put("res", response.toString());

		printFooter();

		return model;
	}
	
    
	private void handleDiff(JSONObject jsonDiff, Status status, Map<String, Object> model) throws Exception {
		if (jsonDiff.has( "workspace1" ) && jsonDiff.has("workspace2")) {
		    JSONObject srcJson = jsonDiff.getJSONObject( "workspace1" );
		    JSONObject targetJson = jsonDiff.getJSONObject( "workspace2" );
		    
		    if (srcJson.has( "id" ) && targetJson.has("id")) {
                String srcWsId = jsonDiff.getString( "id" );
                WorkspaceNode srcWs = WorkspaceNode.getWorkspaceFromId( srcWsId, services, response, responseStatus, null );
                
		        String targetWsId = jsonDiff.getString( "id" );
	            WorkspaceNode targetWs = WorkspaceNode.getWorkspaceFromId( targetWsId, services, response, responseStatus, null );
	            
	            JSONObject top = new JSONObject();
	            JSONArray elements = new JSONArray();
	            if (jsonDiff.has( "addedElements" )) {
	                JSONArray added = jsonDiff.getJSONArray("addedElements");
	                for (int ii = 0; ii < elements.length(); ii++) elements.put( added.getJSONObject( ii ) );
	            }
	            if (jsonDiff.has( "updatedElements" )) {
                    JSONArray updated = jsonDiff.getJSONArray("updatedElements");
                    for (int ii = 0; ii < elements.length(); ii++) elements.put( updated.getJSONObject( ii ) );
	            }
	            top.put( "elements", elements );
	            
	            handleUpdate( top, status, targetWs, false, model );
	            
	            if (jsonDiff.has( "deletedElements" )) {
	                JSONArray deleted = jsonDiff.getJSONArray( "deletedElements" );
	                MmsModelDelete deleteService = new MmsModelDelete(repository, services);
	                deleteService.setWsDiff( targetWs );
                    for (int ii = 0; ii < deleted.length(); ii++) {
                        String id = ((JSONObject)deleted.get(ii)).getString( "sysmlid" );
                        EmsScriptNode root = NodeUtil.findScriptNodeById( id, targetWs, null, false, services, response );
                        deleteService.handleElementHierarchy( root, targetWs, false );
                    }
	            }
	            
	            CommitUtil.merge( jsonDiff, srcWs, targetWs, null, null, false, services, response );
		    }
		}
	}	
}
