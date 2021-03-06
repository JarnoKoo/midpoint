/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Experimental. TODO implement correctly
 *
 * @author mederly
 */
public class DeputyUtils {

	@NotNull
	public static Collection<PrismReferenceValue> getDelegatorReferences(@NotNull UserType user,
			@NotNull RelationRegistry relationRegistry) {
		return user.getDelegatedRef().stream()
				.filter(ref -> relationRegistry.isDelegation(ref.getRelation()))
				.map(ref -> ref.asReferenceValue().clone())
				.collect(Collectors.toList());
	}

	@NotNull
	public static Collection<String> getDelegatorOids(@NotNull UserType user, @NotNull RelationRegistry relationRegistry) {
		return getDelegatorReferences(user, relationRegistry).stream()
				.map(PrismReferenceValue::getOid)
				.collect(Collectors.toList());
	}

	public static boolean isDelegationPresent(@NotNull UserType deputy, @NotNull String delegatorOid,
			@NotNull RelationRegistry relationRegistry) {
		return getDelegatorOids(deputy, relationRegistry).contains(delegatorOid);
	}

	public static boolean isDelegationAssignment(AssignmentType assignment, @NotNull RelationRegistry relationRegistry) {
		return assignment != null
				&& assignment.getTargetRef() != null
				&& relationRegistry.isDelegation(assignment.getTargetRef().getRelation());
	}

	public static boolean isDelegationPath(@NotNull AssignmentPath assignmentPath, @NotNull RelationRegistry relationRegistry) {
		for (AssignmentPathSegment segment : assignmentPath.getSegments()) {
			if (!isDelegationAssignment(segment.getAssignment(), relationRegistry)) {
				return false;
			}
		}
		return true;
	}

	public static List<OtherPrivilegesLimitationType> extractLimitations(AssignmentPath assignmentPath) {
		List<OtherPrivilegesLimitationType> rv = new ArrayList<>();
		for (AssignmentPathSegment segment : assignmentPath.getSegments()) {
			CollectionUtils.addIgnoreNull(rv, segment.getAssignment().getLimitOtherPrivileges());
		}
		return rv;
	}

	public static boolean limitationsAllow(List<OtherPrivilegesLimitationType> limitations, QName itemName) {
		for (OtherPrivilegesLimitationType limitation : limitations) {
			@SuppressWarnings({ "unchecked", "raw" })
			PrismContainer<WorkItemSelectorType> selector = limitation.asPrismContainerValue().findContainer(itemName);
			if (selector == null || selector.isEmpty() || !selector.getRealValue().isAll()) {
				return false;
			}
		}
		return true;
	}

	public static boolean limitationsAllow(List<OtherPrivilegesLimitationType> limitations, QName itemName,
			AbstractWorkItemType workItem) {
		return limitationsAllow(limitations, itemName);			// temporary solution; we do not use work items selectors yet
	}
}
