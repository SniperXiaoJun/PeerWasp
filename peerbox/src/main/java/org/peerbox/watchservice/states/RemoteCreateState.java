package org.peerbox.watchservice.states;

import java.nio.file.Path;

import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.peerbox.app.manager.file.IFileManager;
import org.peerbox.exceptions.NotImplException;
import org.peerbox.watchservice.IAction;
import org.peerbox.watchservice.conflicthandling.ConflictHandler;
import org.peerbox.watchservice.states.listeners.RemoteFileAddListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCreateState extends AbstractActionState {

	private static final Logger logger = LoggerFactory.getLogger(RemoteCreateState.class);

	private boolean localCreateHappened = false;
	public RemoteCreateState(IAction action) {
		super(action, StateType.REMOTE_CREATE);
	}

	@Override
	public AbstractActionState changeStateOnLocalCreate() {
		logger.debug("Local Create Event in RemoteCreateState!  ({}) {}",
				action.getFile().getPath(), action.hashCode());

		action.getFile().updateContentHash();//updateContentHash();
		return new EstablishedState(action);
	}

	@Override
	public AbstractActionState changeStateOnRemoteUpdate() {
		logger.debug("Remote Update Event:  ({})", action.getFile().getPath());
		return this;
	}

	@Override
	public AbstractActionState handleLocalCreate() {
		action.getFile().updateContentHash();//updateContentHash();
		return changeStateOnLocalCreate();
	}

	@Override
	public AbstractActionState handleLocalDelete() {
		return changeStateOnLocalDelete();
	}

	@Override
	public AbstractActionState handleLocalUpdate() {
		ConflictHandler.resolveConflict(action.getFile().getPath());
		return changeStateOnLocalUpdate();
	}

//	@Override
//	public AbstractActionState handleLocalMove(Path oldPath) {
//		return changeStateOnLocalMove(oldPath);
//	}

	@Override
	public AbstractActionState handleRemoteCreate() {
		return changeStateOnRemoteCreate();
	}

	@Override
	public AbstractActionState handleRemoteDelete() {
		return changeStateOnRemoteDelete();
	}

	@Override
	public AbstractActionState handleRemoteUpdate() {
		throw new NotImplException("RemoteCreateState.handleRemoteUpdate");
	}

	@Override
	public AbstractActionState handleRemoteMove(Path path) {
		return changeStateOnRemoteMove(path);
	}

	@Override
	public ExecutionHandle execute(IFileManager fileManager) throws InvalidProcessStateException,
			ProcessExecutionException, NoSessionException, NoPeerConnectionException {
		final Path path = action.getFile().getPath();
		logger.debug("Execute REMOTE ADD, download the file: {}", path);
		handle = fileManager.download(path);
		if (handle != null && handle.getProcess() != null) {
			handle.getProcess().attachListener(new RemoteFileAddListener(path, action.getFileEventManager().getMessageBus()));
			handle.executeAsync();
		} else {
			System.err.println("process or handle is null");
		}
		return new ExecutionHandle(action, handle);
	}
	
	public boolean localCreateHappened(){
		return localCreateHappened;
	}

	public void setLocalCreateHappened(boolean b) {
		localCreateHappened = b;
	}

}
