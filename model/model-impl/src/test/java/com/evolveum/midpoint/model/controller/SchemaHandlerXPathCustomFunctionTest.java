/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.controller;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Igor Farinic
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class SchemaHandlerXPathCustomFunctionTest {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlerXPathCustomFunctionTest.class);
	@Autowired
	private SchemaHandler schemaHandler;

	@Test
	@SuppressWarnings("unchecked")
	public void testCustomFunction() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/account-custom-function.xml"))).getValue();
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/user-custom-function.xml"))).getValue();

		OperationResult result = new OperationResult("testCustomFunction");
		ObjectModificationType changes = schemaHandler.processOutboundHandling(user, account, result);
		LOGGER.info(result.debugDump());

		ResourceObjectShadowType appliedAccountShadow = ModelTUtil.patchXml(changes, account,
				AccountShadowType.class);

		assertEquals("__NAME__", appliedAccountShadow.getAttributes().getAny().get(0).getLocalName());
		assertEquals("Bond", appliedAccountShadow.getAttributes().getAny().get(0).getTextContent());
	}
}
