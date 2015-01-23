package org.peerbox.watchservice.states;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.peerbox.app.manager.ProcessHandle;
import org.peerbox.app.manager.file.IFileManager;
import org.peerbox.exceptions.NotImplException;
import org.peerbox.watchservice.Action;
import org.peerbox.watchservice.IFileEventManager;
import org.peerbox.watchservice.filetree.composite.FileComponent;
import org.peerbox.watchservice.filetree.composite.FolderComposite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.SetMultimap;

/**
 * Interface for different states of implemented state pattern
 *
 * @author winzenried
 *
 */
public abstract class AbstractActionState {
	private final static Logger logger = LoggerFactory.getLogger(AbstractActionState.class);
	protected Action action;
	protected StateType type = StateType.ABSTRACT;
	protected ProcessHandle<Void> handle;

	public AbstractActionState(Action action, StateType type) {
		this.action = action;
		this.type = type;
	}

	public StateType getStateType(){
		return type;
	}

	public AbstractActionState getDefaultState(){
		return new EstablishedState(action);
	}

	public void updateTimeAndQueue(){
		action.getEventManager().getFileComponentQueue().remove(action.getFile());
		action.updateTimestamp();
		action.getEventManager().getFileComponentQueue().add(action.getFile());
	}

	protected void logStateTransission(StateType stateBefore, EventType event, StateType stateAfter){
		logger.debug("File {}: {} + {}  --> {}", action.getFile().getPath(),
				stateBefore.getString(), event.getString(), stateAfter.getString());
	}

	public void performCleanup(){
		//nothing to do by default!
	}

	/*
	 * LOCAL state changers
	 */
	public AbstractActionState changeStateOnLocalCreate(){
		logStateTransission(getStateType(), EventType.LOCAL_CREATE, StateType.LOCAL_CREATE);
		return new LocalCreateState(action);
	}

	public AbstractActionState changeStateOnLocalDelete(){
		return new InitialState(action);
	}

	public AbstractActionState changeStateOnLocalUpdate(){
		return new LocalUpdateState(action);
	}

	public AbstractActionState changeStateOnLocalMove(Path oldPath){
		throw new NotImplException(action.getCurrentState().getStateType().getString() + ".changeStateOnLocalMove");
	}

	public AbstractActionState changeStateOnLocalHardDelete(){
		return new LocalHardDeleteState(action);
	}

	/*
	 * REMOTE state changers
	 */
	public AbstractActionState changeStateOnRemoteDelete(){
		return new InitialState(action);
	}

	public AbstractActionState changeStateOnRemoteCreate(){
		return new RemoteCreateState(action);
	}

	public AbstractActionState changeStateOnRemoteUpdate(){
		return new RemoteUpdateState(action);
	}

	public AbstractActionState changeStateOnRemoteMove(Path oldFilePath){
		throw new NotImplException(action.getCurrentState().getStateType().getString() + ".changeStateOnRemoteMove");
	}

	/*
	 * LOCAL event handler
	 */

	public abstract AbstractActionState handleLocalCreate();

	public AbstractActionState handleLocalHardDelete(){
		logger.trace("File {}: entered handleLocalHardDelete", action.getFile().getPath());
		updateTimeAndQueue();
		return changeStateOnLocalHardDelete();
	}

	public AbstractActionState handleLocalDelete(){
		IFileEventManager eventManager = action.getEventManager();
		eventManager.getFileComponentQueue().remove(action.getFile());
		eventManager.getFileTree().deleteFile(action.getFile().getPath());
		action.getFile().setIsSynchronized(false);
		logger.debug("Deleted {} from tree.", action.getFile().getPath());
		if(action.getFile().isFile()){
//			String oldHash = action.getFile().getContentHash();
//			logger.debug("File: {}Previous content hash: {} new content hash: ", action.getFilePath(), oldHash, action.getFile().getContentHash());
			SetMultimap<String, FileComponent> deletedFiles = action.getEventManager().getFileTree().getDeletedByContentHash();
			deletedFiles.put(action.getFile().getContentHash(), action.getFile());
//			logger.debug("Put deleted file {} with hash {} to SetMultimap<String, FileComponent>", action.getFilePath(), action.getFile().getContentHash());
		} else {
			Map<String, FolderComposite> deletedFolders = eventManager.getFileTree().getDeletedByContentNamesHash();
			deletedFolders.put(action.getFile().getStructureHash(), (FolderComposite)action.getFile());
		}
		return this.changeStateOnLocalDelete();
	}

	public AbstractActionState handleLocalUpdate() {
		updateTimeAndQueue();
		return changeStateOnLocalUpdate();
	}


	public AbstractActionState handleLocalMove(Path oldFilePath){
		return changeStateOnLocalMove(oldFilePath);
	}

	public AbstractActionState handleLocalRecover(File currentFile, int version){
		throw new NotImplException("Recovery Event occured in invalid state!");
	}

	/*
	 * REMOTE event handler
	 */

	public AbstractActionState handleRemoteCreate(){
		return changeStateOnRemoteCreate();
	}

	public AbstractActionState handleRemoteDelete() {
		logger.debug("EstablishedState.handleRemoteDelete");
		IFileEventManager eventManager = action.getEventManager();
		eventManager.getFileTree().deleteFile(action.getFile().getPath());
		eventManager.getFileComponentQueue().remove(action.getFile());

		try {
			java.nio.file.Files.delete(action.getFile().getPath());
		} catch (IOException e) {
			logger.warn("Could not delete file {} ({}).",
					action.getFile().getPath(), e.getMessage(), e);
		}
		return changeStateOnRemoteDelete();
	}

	public AbstractActionState handleRemoteUpdate() {
		updateTimeAndQueue();
		return changeStateOnRemoteUpdate();
	}

	public AbstractActionState handleRemoteMove(Path path){
		return changeStateOnRemoteMove(path);
	}

	/*
	 * Execution and notification related functions
	 */

	public abstract ExecutionHandle execute(IFileManager fileManager) throws NoSessionException,
			NoPeerConnectionException, InvalidProcessStateException, ProcessExecutionException;

}