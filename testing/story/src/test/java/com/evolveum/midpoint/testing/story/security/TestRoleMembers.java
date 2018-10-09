/*
 * Copyright (c) 2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.testing.story.security;

import static org.testng.AssertJUnit.assertEquals;
import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests for privacy-enhancing setup. E.g. broad get authorizations, but limited search.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRoleMembers extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "security/role-members");
	
	protected static final File USERS_ROLES_FILE = new File(TEST_DIR, "users-roles.xml");
	
	protected static final String USER_GUYBRUSH_OID = "df294d8e-cafc-11e8-bb75-6b3aee37f782";
	protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
	
	protected static final String USER_ELAINE_OID = "e7b4bc54-cafc-11e8-a0e8-cf8010206061";
	protected static final String USER_ELAINE_USERNAME = "elaine";
	
	protected static final String USER_MANCOMB_OID = "f034084e-cafc-11e8-a31f-f7b3274f7480";
	protected static final String USER_MANCOMB_USERNAME = "mancomb";

	protected static final String ROLE_END_USER_OID = "c057bbd0-cafb-11e8-9525-cbcb025548f7";
	
	protected static final String ROLE_GOVERNOR_OID = "78a76270-cafd-11e8-ba0c-4f7b8e8b4e57";
	
	protected static final String ROLE_PIRATE_OID = "31d5bdce-cafd-11e8-b41d-b373e6c564cb";
	protected static final String ROLE_PIRATE_NAME = "Pirate";
	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		importObjectsFromFileNotRaw(USERS_ROLES_FILE, initTask, initResult);
	}
	
	
	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);
		
		// WHEN
        displayWhen(TEST_NAME);

        assertUserBefore(USER_GUYBRUSH_OID)
			.assertName(USER_GUYBRUSH_USERNAME)
			.roleMembershipRefs()
	    		.assertRole(ROLE_END_USER_OID, SchemaConstants.ORG_DEFAULT)
	    		.assertRoleMemberhipRefs(1);
        
        assertUserBefore(USER_ELAINE_OID)
			.assertName(USER_ELAINE_USERNAME)
			.roleMembershipRefs()
	    		.assertRole(ROLE_PIRATE_OID, SchemaConstants.ORG_OWNER)
	    		.assertRole(ROLE_GOVERNOR_OID, SchemaConstants.ORG_DEFAULT)
	    		.assertRole(ROLE_END_USER_OID, SchemaConstants.ORG_DEFAULT)
	    		.assertRoleMemberhipRefs(3);
        
        assertUserBefore(USER_MANCOMB_OID)
			.assertName(USER_MANCOMB_USERNAME)
			.roleMembershipRefs()
	    		.assertRole(ROLE_PIRATE_OID, SchemaConstants.ORG_DEFAULT)
	    		.assertRoleMemberhipRefs(1);
		
        assertCanSearchPirateMembers(true);
        
		// THEN
		displayThen(TEST_NAME);
		
	}

	/**
	 * MID-4893
	 */
	@Test
	public void test100AutzGuybrushNoMembers() throws Exception {
		final String TEST_NAME = "test100AutzGuybrushNoMembers";
		displayTestTitle(TEST_NAME);
		
		login(USER_GUYBRUSH_USERNAME);

		// WHEN
        displayWhen(TEST_NAME);

        PrismObject<UserType> userMancomb = assertGetAllow(UserType.class, USER_MANCOMB_OID);
        assertUser(userMancomb, "mancomb")
        	.assertName(USER_MANCOMB_USERNAME)
        	.assertRoleMemberhipRefs(0);
        
        assertCanSearchPirateMembers(false);
		
		// THEN
		displayThen(TEST_NAME);
		
	}
	
	/**
	 * MID-4893
	 */
	@Test
	public void test105AutzElaineMembers() throws Exception {
		final String TEST_NAME = "test105AutzElaineMembers";
		displayTestTitle(TEST_NAME);
		
		login(USER_ELAINE_USERNAME);

		// WHEN
        displayWhen(TEST_NAME);

        PrismObject<UserType> userMancomb = assertGetAllow(UserType.class, USER_MANCOMB_OID);
        assertUser(userMancomb, "mancomb")
    		.assertName(USER_MANCOMB_USERNAME)
    		.roleMembershipRefs()
        		.assertRole(ROLE_PIRATE_OID)
        		.assertRoleMemberhipRefs(1);
        
        display("HEREHERE");
        assertCanSearchPirateMembers(true);
		
		// THEN
		displayThen(TEST_NAME);
		
	}
	
	private void assertCanSearchPirateMembers(boolean expected) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		assertEquals("Wrong canSearch on pirate members", expected, canSearchPirateMembers());
	}
	
	private boolean canSearchPirateMembers() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(TestRoleMembers.class.getName() + ".canSearchPirateMembers");
        OperationResult result = task.getResult();
		ObjectQuery query = queryFor(UserType.class)
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(ROLE_PIRATE_OID).build();
		// Object is null here by purpose. Maybe the object does not really makes any sense in canSearch() ?
        boolean canSearch = modelInteractionService.canSearch(UserType.class, null, null, false, query, task, result);
        assertSuccess(result);
		return canSearch;
	}
	
}
