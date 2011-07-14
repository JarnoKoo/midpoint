/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.task.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * Implementation of a Task.
 * 
 * This is very simplistic now. It does not even serialize itself.
 * 
 * @see TaskManagerImpl
 * 
 * @author Radovan Semancik
 *
 */
public class TaskImpl implements Task {
	
	private TaskExecutionStatus executionStatus;
	private TaskExclusivityStatus exclusivityStatus;
	private TaskPersistenceStatus persistenceStatus;
	private String handlerUri;
	private ObjectType object;
	private String oid;
	private String name;
	private Long lastRunStartTimestamp;
	private Long lastRunFinishTimestamp;
	private List<Property> extension;
	private long progress;
	private RepositoryService repositoryService;

	/**
	 * Note: This constructor assumes that the task is transient.
	 * @param taskType
	 * @param repositoryService
	 */	
	TaskImpl() {
		executionStatus = TaskExecutionStatus.RUNNING;
		exclusivityStatus = TaskExclusivityStatus.CLAIMED;
		persistenceStatus = TaskPersistenceStatus.TRANSIENT;
		extension = new ArrayList<Property>();
		progress = 0;
		repositoryService = null;
	}

	/**
	 * Note: This constructor assumes that the task is persistent.
	 * @param taskType
	 * @param repositoryService
	 */
	TaskImpl(TaskType taskType, RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
		executionStatus = TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
		exclusivityStatus = TaskExclusivityStatus.fromTaskType(taskType.getExclusivityStatus());
		// If that is created from the TaskType, then this is persistent task
		persistenceStatus = TaskPersistenceStatus.PERSISTENT;
		oid = taskType.getOid();
		handlerUri = taskType.getHandlerUri();
		// TODO: object = 
		name = taskType.getName();
		if (taskType.getLastRunStartTimestamp()!=null) {
			lastRunStartTimestamp = new Long(XsdTypeConverter.toMillis(taskType.getLastRunStartTimestamp()));
		}
		if (taskType.getLastRunFinishTimestamp()!=null) {
			lastRunFinishTimestamp = new Long(XsdTypeConverter.toMillis(taskType.getLastRunFinishTimestamp()));
		}
		if (taskType.getProgress()!=null) {
			progress = taskType.getProgress().longValue();
		} else {
			progress = 0;
		}
		// TODO: extension
	}
	
	RepositoryService getRepositoryService() {
		return repositoryService;
	}
	
	void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getExecutionStatus()
	 */
	@Override
	public TaskExecutionStatus getExecutionStatus() {
		return executionStatus;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getPersistenceStatus()
	 */
	@Override
	public TaskPersistenceStatus getPersistenceStatus() {
		return persistenceStatus;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getExclusivityStatus()
	 */
	@Override
	public TaskExclusivityStatus getExclusivityStatus() {
		return exclusivityStatus;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#isAsynchronous()
	 */
	@Override
	public boolean isAsynchronous() {
		// This is very simple now. It may complicate later.
		return (persistenceStatus==TaskPersistenceStatus.PERSISTENT);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getObject()
	 */
	@Override
	public ObjectType getObject() {
		return object;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getResult()
	 */
	@Override
	public OperationResult getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHanderUri() {
		return handlerUri;
	}

	@Override
	public void setHanderUri(String handlerUri) {
		this.handlerUri = handlerUri;
	}

	@Override
	public void setExecutionStatus(TaskExecutionStatus executionStatus) {
		this.executionStatus = executionStatus;
	}

	@Override
	public void setPersistenceStatus(TaskPersistenceStatus persistenceStatus) {
		this.persistenceStatus = persistenceStatus;
	}

	@Override
	public void setExclusivityStatus(TaskExclusivityStatus exclusivityStatus) {
		this.exclusivityStatus = exclusivityStatus;
	}

	@Override
	public String getOid() {
		return oid;
	}

	@Override
	public void setOid(String oid) {
		this.oid = oid;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public List<Property> getExtension() {
		return extension;
	}

	@Override
	public void modifyExtension(PropertyModification modification) {
		throw new NotImplementedException();
	}
	
	@Override
	public TaskType getTaskTypeObject() {
		throw new NotImplementedException();
	}

	@Override
	public Long getLastRunStartTimestamp() {
		return lastRunStartTimestamp;
	}

	@Override
	public Long getLastRunFinishTimestamp() {
		return lastRunFinishTimestamp;
	}

	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("Task(");
		sb.append(TaskImpl.class.getName());
		sb.append(")\n");
		sb.append("  OID: ");
		sb.append(oid);
		sb.append("\n  name: ");
		sb.append(name);
		sb.append("\n  executionStatus: ");
		sb.append(executionStatus);
		sb.append("\n  exclusivityStatus: ");
		sb.append(exclusivityStatus);
		sb.append("\n  persistenceStatus: ");
		sb.append(persistenceStatus);
		sb.append("\n  handlerUri: ");
		sb.append(handlerUri);
		sb.append("\n  object: ");
		sb.append(object);
		sb.append("\n  lastRunStartTimestamp: ");
		sb.append(lastRunStartTimestamp);
		sb.append("\n  lastRunFinishTimestamp: ");
		sb.append(lastRunFinishTimestamp);
		sb.append("\n  progress: ");
		sb.append(progress);
		sb.append("\n  extension: ");
		sb.append(extension);
		return sb.toString();
	}

	@Override
	public void recordRunStart(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO 
		lastRunStartTimestamp = System.currentTimeMillis();
		// This is all we need to do for transient tasks
		if (!isPersistent()) {
			return;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis(lastRunStartTimestamp);
		ObjectModificationType modification = ObjectTypeUtil.createModificationReplaceProperty(oid, SchemaConstants.C_TASK_LAST_RUN_START_TIMESTAMP, cal);
		repositoryService.modifyObject(modification, parentResult);
	}

	@Override
	public void recordRunFinish(TaskRunResult runResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO
		progress = runResult.getProgress(); 
		lastRunFinishTimestamp = System.currentTimeMillis();
		// This is all we need to do for transient tasks
		if (!isPersistent()) {
			return;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis(lastRunFinishTimestamp);
		ObjectModificationType modification = new ObjectModificationType();
		modification.setOid(oid);
		PropertyModificationType timestampModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_LAST_RUN_FINISH_TIMESTAMP, cal);
		modification.getPropertyModification().add(timestampModification);
		PropertyModificationType progressModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_PROGRESS, progress);
		modification.getPropertyModification().add(progressModification);
		PropertyModificationType resultModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_RESULT, parentResult.createOperationResultType());
		modification.getPropertyModification().add(resultModification);
		repositoryService.modifyObject(modification, parentResult);
		// TODO: Also save the OpResult
	}

	
	private boolean isPersistent() {
		return persistenceStatus == TaskPersistenceStatus.PERSISTENT;
	}

	@Override
	public long getProgress() {
		return progress;
	}

}
