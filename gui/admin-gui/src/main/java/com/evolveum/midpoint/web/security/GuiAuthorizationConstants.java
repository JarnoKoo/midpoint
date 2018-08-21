package com.evolveum.midpoint.web.security;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.security.api.AuthorizationConstants;

public class GuiAuthorizationConstants {

	public static final Map<String, String> ROLE_MEMBERS_AUTHORIZATIONS = new HashMap<>();
	public static final Map<String, String> SERVICE_MEMBERS_AUTHORIZATIONS = new HashMap<>();
	public static final Map<String, String> ORG_MEMBERS_AUTHORIZATIONS = new HashMap<>();
	public static final Map<String, String> GOVERNANCE_MEMBERS_AUTHORIZATIONS = new HashMap<>();
	
	public static final String MEMBER_OPERATION_ASSIGN = "assign";
	public static final String MEMBER_OPERATION_UNASSIGN = "unassign";
	public static final String MEMBER_OPERATION_RECOMPUTE = "recompute";
	public static final String MEMBER_OPERATION_CREATE = "create";
	
	static {
		ROLE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_ASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
		ROLE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_UNASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI);
		ROLE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_RECOMPUTE, AuthorizationConstants.AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_URI);
		ROLE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_CREATE, AuthorizationConstants.AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_URI);
	}
	
	static {
		SERVICE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_ASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
		SERVICE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_UNASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI);
		SERVICE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_RECOMPUTE, AuthorizationConstants.AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_URI);
		SERVICE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_CREATE, AuthorizationConstants.AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_URI);
	}
	
	static {
		ORG_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_ASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
		ORG_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_UNASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI);
		ORG_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_RECOMPUTE, AuthorizationConstants.AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_URI);
		ORG_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_CREATE, AuthorizationConstants.AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_URI);
	}
	
	static {
		GOVERNANCE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_ASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
		GOVERNANCE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_UNASSIGN, AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI);
		GOVERNANCE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_RECOMPUTE, AuthorizationConstants.AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_URI);
		GOVERNANCE_MEMBERS_AUTHORIZATIONS.put(MEMBER_OPERATION_CREATE, AuthorizationConstants.AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_URI);
	}
}
