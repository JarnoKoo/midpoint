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

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaHelper;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalSpecificContent;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.velocity.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectName;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectOid;

/**
 * Part of PolicyRuleBasedAspect related to assignments.
 *
 * @author mederly
 */
@Component
public class AssignmentPolicyAspectPart {

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentPolicyAspectPart.class);

	@Autowired private PolicyRuleBasedAspect main;
	@Autowired protected ApprovalSchemaHelper approvalSchemaHelper;
	@Autowired protected MiscDataUtil miscDataUtil;
	@Autowired protected PrismContext prismContext;
	@Autowired protected ItemApprovalProcessInterface itemApprovalProcessInterface;
	@Autowired protected BaseConfigurationHelper baseConfigurationHelper;

	void extractAssignmentBasedInstructions(ObjectTreeDeltas<?> objectTreeDeltas, PrismObject<UserType> requester,
			List<PcpChildWfTaskCreationInstruction<?>> instructions, ModelInvocationContext ctx, OperationResult result)
			throws SchemaException {

		DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = ((LensContext<?>) ctx.modelContext).getEvaluatedAssignmentTriple();
		LOGGER.trace("Processing evaluatedAssignmentTriple:\n{}", DebugUtil.debugDumpLazily(evaluatedAssignmentTriple));
		if (evaluatedAssignmentTriple == null) {
			return;
		}

		for (EvaluatedAssignment<?> newAssignment : evaluatedAssignmentTriple.getPlusSet()) {
			CollectionUtils.addIgnoreNull(instructions,
					createInstructionFromAssignment(newAssignment, PlusMinusZero.PLUS, objectTreeDeltas, requester, ctx, result));
		}
		for (EvaluatedAssignment<?> newAssignment : evaluatedAssignmentTriple.getMinusSet()) {
			CollectionUtils.addIgnoreNull(instructions,
					createInstructionFromAssignment(newAssignment, PlusMinusZero.MINUS, objectTreeDeltas, requester, ctx, result));
		}
		// Note: to implement assignment modifications we would need to fix subtractFromModification method below
	}

	private PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> createInstructionFromAssignment(
			EvaluatedAssignment<?> evaluatedAssignment, PlusMinusZero assignmentMode, @NotNull ObjectTreeDeltas<?> objectTreeDeltas,
			PrismObject<UserType> requester, ModelInvocationContext ctx, OperationResult result) throws SchemaException {

		assert assignmentMode == PlusMinusZero.PLUS || assignmentMode == PlusMinusZero.MINUS;

		// We collect all target rules; hoping that only relevant ones are triggered.
		// For example, if we have assignment policy rule on induced role, it will get here.
		// But projector will take care not to trigger it unless the rule is capable (e.g. configured)
		// to be triggered in such a situation
		List<EvaluatedPolicyRule> triggeredApprovalActionRules = main.selectTriggeredApprovalActionRules(evaluatedAssignment.getAllTargetsPolicyRules());
		logApprovalActions(evaluatedAssignment, triggeredApprovalActionRules, assignmentMode);

		// Currently we can deal only with assignments that have a specific target
		PrismObject<?> targetObject = evaluatedAssignment.getTarget();
		if (targetObject == null) {
			if (!triggeredApprovalActionRules.isEmpty()) {
				throw new IllegalStateException("No target in " + evaluatedAssignment + ", but with "
						+ triggeredApprovalActionRules.size() + " triggered approval action rule(s)");
			} else {
				return null;
			}
		}

		// Let's construct the approval schema plus supporting triggered approval policy rule information
		ApprovalSchemaBuilder.Result approvalSchemaResult = createSchemaWithRules(triggeredApprovalActionRules, assignmentMode,
				evaluatedAssignment, ctx, result);
		if (approvalSchemaHelper.shouldBeSkipped(approvalSchemaResult.schemaType)) {
			return null;
		}

		// Cut assignment from delta, prepare task instruction
		@SuppressWarnings("unchecked")
		PrismContainerValue<AssignmentType> assignmentValue = evaluatedAssignment.getAssignmentType().asPrismContainerValue();
		boolean assignmentRemoved;
		switch (assignmentMode) {
			case PLUS: assignmentRemoved = false; break;
			case MINUS: assignmentRemoved = true; break;
			default: throw new UnsupportedOperationException("Processing assignment zero set is not yet supported.");
		}
		boolean removed = objectTreeDeltas.subtractFromFocusDelta(new ItemPath(FocusType.F_ASSIGNMENT), assignmentValue, assignmentRemoved,
				false);
		if (!removed) {
			ObjectDelta<?> secondaryDelta = ctx.modelContext.getFocusContext().getSecondaryDelta();
			if (secondaryDelta != null && secondaryDelta.subtract(new ItemPath(FocusType.F_ASSIGNMENT), assignmentValue, assignmentRemoved, true)) {
				LOGGER.trace("Assignment to be added/deleted was not found in primary delta. It is present in secondary delta, so there's nothing to be approved.");
				return null;
			}
			String message = "Assignment to be added/deleted was not found in primary nor secondary delta."
					+ "\nAssignment:\n" + assignmentValue.debugDump()
					+ "\nPrimary delta:\n" + objectTreeDeltas.debugDump();
			throw new IllegalStateException(message);
		}
		ObjectDelta<? extends ObjectType> focusDelta = objectTreeDeltas.getFocusChange();
		if (focusDelta.isAdd()) {
			miscDataUtil.generateFocusOidIfNeeded(ctx.modelContext, focusDelta);
		}
		return prepareAssignmentRelatedTaskInstruction(approvalSchemaResult, evaluatedAssignment, assignmentRemoved, ctx.modelContext, requester, result);
	}

	private void logApprovalActions(EvaluatedAssignment<?> newAssignment,
			List<EvaluatedPolicyRule> triggeredApprovalActionRules, PlusMinusZero plusMinusZero) {
		if (LOGGER.isDebugEnabled() && !triggeredApprovalActionRules.isEmpty()) {
			LOGGER.trace("-------------------------------------------------------------");
			LOGGER.debug("Assignment to be {}: {}: {} this target policy rules, {} triggered approval actions:",
					plusMinusZero == PlusMinusZero.PLUS ? "added" : "deleted",
					newAssignment, newAssignment.getThisTargetPolicyRules().size(), triggeredApprovalActionRules.size());
			for (EvaluatedPolicyRule t : triggeredApprovalActionRules) {
				LOGGER.debug(" - Approval actions: {}", t.getActions().getApproval());
				for (EvaluatedPolicyRuleTrigger trigger : t.getTriggers()) {
					LOGGER.debug("   - {}", trigger);
				}
			}
		}
	}

	private ApprovalSchemaBuilder.Result createSchemaWithRules(List<EvaluatedPolicyRule> triggeredApprovalRules,
			PlusMinusZero assignmentMode, @NotNull EvaluatedAssignment<?> evaluatedAssignment, ModelInvocationContext ctx,
			OperationResult result) throws SchemaException {

		PrismObject<?> targetObject = evaluatedAssignment.getTarget();
		ApprovalSchemaBuilder builder = new ApprovalSchemaBuilder(main, approvalSchemaHelper);

		// (1) legacy approvers (only if adding)
		LegacyApproversSpecificationUsageType configuredUseLegacyApprovers =
				baseConfigurationHelper.getUseLegacyApproversSpecification(ctx.wfConfiguration);
		boolean useLegacyApprovers = configuredUseLegacyApprovers == LegacyApproversSpecificationUsageType.ALWAYS
				|| configuredUseLegacyApprovers == LegacyApproversSpecificationUsageType.IF_NO_EXPLICIT_APPROVAL_POLICY_ACTION
				&& triggeredApprovalRules.isEmpty();

		if (assignmentMode == PlusMinusZero.PLUS && useLegacyApprovers && targetObject.asObjectable() instanceof AbstractRoleType) {
			AbstractRoleType abstractRole = (AbstractRoleType) targetObject.asObjectable();
			if (abstractRole.getApprovalSchema() != null) {
				builder.addPredefined(targetObject, abstractRole.getApprovalSchema().clone());
				LOGGER.trace("Added legacy approval schema for {}", evaluatedAssignment);
			} else if (!abstractRole.getApproverRef().isEmpty() || !abstractRole.getApproverExpression().isEmpty()) {
				ApprovalStageDefinitionType level = new ApprovalStageDefinitionType(prismContext);
				level.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(abstractRole.getApproverRef()));
				level.getApproverExpression().addAll(CloneUtil.cloneCollectionMembers(abstractRole.getApproverExpression()));
				level.setAutomaticallyApproved(abstractRole.getAutomaticallyApproved());
				// consider default (if expression returns no approvers) -- currently it is "reject"; it is probably correct
				builder.addPredefined(targetObject, level);
				LOGGER.trace("Added legacy approval schema (from approverRef, approverExpression, automaticallyApproved) for {}", evaluatedAssignment);
			}
		}

		// (2) default policy action (only if adding)
		if (triggeredApprovalRules.isEmpty() && assignmentMode == PlusMinusZero.PLUS
				&& baseConfigurationHelper.getUseDefaultApprovalPolicyRules(ctx.wfConfiguration) != DefaultApprovalPolicyRulesUsageType.NEVER) {
			if (builder.addPredefined(targetObject, SchemaConstants.ORG_APPROVER, result)) {
				LOGGER.trace("Added default approval action, as no explicit one was found for {}", evaluatedAssignment);
			}
		}

		// (3) actions from triggered rules
		for (EvaluatedPolicyRule approvalRule : triggeredApprovalRules) {
			for (ApprovalPolicyActionType approvalAction : approvalRule.getActions().getApproval()) {
				builder.add(main.getSchemaFromAction(approvalAction), approvalAction.getCompositionStrategy(), targetObject, approvalRule);
			}
		}
		return builder.buildSchema(ctx, result);
	}

	private PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> prepareAssignmentRelatedTaskInstruction(
			ApprovalSchemaBuilder.Result builderResult,
			EvaluatedAssignment<?> evaluatedAssignment, boolean assignmentRemoved, ModelContext<?> modelContext,
			PrismObject<UserType> requester, OperationResult result) throws SchemaException {

		String objectOid = getFocusObjectOid(modelContext);
		String objectName = getFocusObjectName(modelContext);

		@SuppressWarnings("unchecked")
		PrismObject<? extends ObjectType> target = (PrismObject<? extends ObjectType>) evaluatedAssignment.getTarget();
		Validate.notNull(target, "assignment target is null");

		String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";
		String operation = (assignmentRemoved
				? "unassigning " + targetName + " from " :
				"assigning " + targetName + " to ")
				+ objectName;
		String approvalTaskName = "Approve " + operation;

		PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> instruction =
				PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(main.getChangeProcessor(), approvalTaskName,
						builderResult.schemaType, builderResult.attachedRules);

		instruction.prepareCommonAttributes(main, modelContext, requester);

		ObjectDelta<? extends FocusType> delta = assignmentToDelta(modelContext.getFocusClass(),
				evaluatedAssignment.getAssignmentType(), assignmentRemoved, objectOid);
		instruction.setDeltasToProcess(delta);

		instruction.setObjectRef(modelContext, result);
		instruction.setTargetRef(createObjectRef(target), result);

		String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
		instruction.setTaskName("Approval " + andExecuting + "of " + operation);
		instruction.setProcessInstanceName(StringUtils.capitalizeFirstLetter(operation));

		itemApprovalProcessInterface.prepareStartInstruction(instruction);

		return instruction;
	}

	// creates an ObjectDelta that will be executed after successful approval of the given assignment
	@SuppressWarnings("unchecked")
	private ObjectDelta<? extends FocusType> assignmentToDelta(Class<? extends Objectable> focusClass,
			AssignmentType assignmentType, boolean assignmentRemoved, String objectOid) throws SchemaException {
		PrismContainerValue value = assignmentType.clone().asPrismContainerValue();
		S_ValuesEntry item = DeltaBuilder.deltaFor(focusClass, prismContext)
				.item(FocusType.F_ASSIGNMENT);
		S_ItemEntry op = assignmentRemoved ? item.delete(value) : item.add(value);
		return (ObjectDelta<? extends FocusType>) op.asObjectDelta(objectOid);
	}

}