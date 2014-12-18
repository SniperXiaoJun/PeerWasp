package org.peerbox.watchservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.peerbox.FileManager;
import org.peerbox.watchservice.states.AbstractActionState;
import org.peerbox.watchservice.states.EstablishedState;
import org.peerbox.watchservice.states.ExecutionHandle;
import org.peerbox.watchservice.states.InitialState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Action class provides a systematic and lose-coupled way to change the 
 * state of an object as part of the chosen state pattern design.
 * 
 * 
 * @author albrecht, anliker, winzenried
 *
 */

public class Action implements IAction{
	private final static Logger logger = LoggerFactory.getLogger(Action.class);
	private long timestamp = Long.MAX_VALUE;
	
	private AbstractActionState currentState;
	private AbstractActionState nextState;
	private Set<IActionEventListener> eventListeners;
	private int executionAttempts = 0;
	private IFileEventManager eventManager;
	private FileComponent file;
	
	private boolean isUploaded = false;
	private boolean isExecuting = false;
	private boolean	changedWhileExecuted = false;
	private final Lock lock = new ReentrantLock();
	
	/**
	 * Initialize with timestamp and set currentState to initial state
	 */
	public Action(IFileEventManager fileEventManager){
		currentState = new InitialState(this);
		nextState = new EstablishedState(this);
		eventListeners = new HashSet<IActionEventListener>();
		this.eventManager = fileEventManager;
		updateTimestamp();
	}
	
	public Action(){
		this(null);
	}
	
	public void setEventManager(IFileEventManager fileEventManager){
		this.eventManager = fileEventManager;
	}
	
	public IFileEventManager getEventManager(){
		return eventManager;
	}
	
	public FileComponent getFile() {
		return file;
	}

	public void setFile(FileComponent file) {
		this.file = file;
	}
	
	public void updateTimestamp() {
		timestamp = System.currentTimeMillis();
	}
	
	public boolean getIsUploaded(){
		return isUploaded;
	}
	
	public void setIsUploaded(boolean isUploaded){
		this.isUploaded = isUploaded;
	}
	
	/**
	 * changes the state of the currentState to Create state if current state allows it.
	 */
	public void handleLocalCreateEvent(){
		logger.trace("handleLocalCreateEvent - File: {}", getFilePath());
		if(isExecuting){
			
			acquireLockOnThis();
			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnLocalCreate();
			checkIfChanged();
			releaseLockOnThis();
		} else {
			updateTimestamp();
			currentState = currentState.handleLocalCreate();
			nextState = currentState.getDefaultState();
		}
	}
	
	private void releaseLockOnThis() {
		// TODO Auto-generated method stub
		logger.trace("File {}: Release lock on this at {}", getFilePath(), System.currentTimeMillis());
		//this.notify();
		lock.unlock();
	}

	private void acquireLockOnThis() {
			logger.trace("File {}: Wait for own lock at {}", getFilePath(), System.currentTimeMillis());
			lock.lock();
			logger.trace("File {}: Received own lock at {}", getFilePath(), System.currentTimeMillis());
	}

	/**
	 * changes the state of the currentState to Modify state if current state allows it.
	 */
	public void handleLocalUpdateEvent(){
		logger.trace("handleLocalUpdateEvent - File: {}", getFilePath());
		acquireLockOnThis();
		if(isExecuting){
			

			logger.trace("Event occured for {} while executing.", getFilePath());
			nextState = nextState.changeStateOnLocalUpdate();
			checkIfChanged();

			logger.trace("Set next state for {} to {}", getFilePath(), nextState.getClass());
		} else {
			updateTimestamp();
			currentState = currentState.handleLocalUpdate();
			nextState = currentState.getDefaultState();
		}
		releaseLockOnThis();
	}

	/**
	 * changes the state of the currentState to Delete state if current state allows it.
	 */
	public void handleLocalDeleteEvent(){
		acquireLockOnThis();
		logger.trace("handleLocalDeleteEvent - File: {}", getFilePath());
		if(isExecuting){

			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnLocalDelete();
			checkIfChanged();

		} else {
			updateTimestamp();
			currentState = currentState.handleLocalDelete();
			nextState = currentState.getDefaultState();

		}
		releaseLockOnThis();
	}
	
	public void handleLocalHardDeleteEvent(){
		if(getFile().isFolder()){
			logger.trace("File {}: is a folder", getFile().getPath());
			FolderComposite folder = (FolderComposite)getFile();
			Map<String, FileComponent> children = folder.getChildren();
			
//			Vector<FileComponent> children = new Vector<FileComponent>(folder.getChildren().values());
			for(Map.Entry<String, FileComponent> childEntry : children.entrySet()){
				FileComponent child = childEntry.getValue();
				logger.trace("Child {}: handleLocalHardDelete", getFile().getPath());
				child.getAction().handleLocalHardDeleteEvent();
			}
		}
		acquireLockOnThis();
		logger.trace("handleLocalHardDeleteEvent - File: {}", getFilePath());
		if(isExecuting){
			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnLocalHardDelete();
			checkIfChanged();

		} else {
			updateTimestamp();
			currentState = currentState.handleLocalHardDelete();
			nextState = currentState.getDefaultState();

		}
		releaseLockOnThis();
		
		try {
			Files.delete(getFilePath());
			logger.trace("DELETED FROM DISK: {}", getFilePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void handleLocalMoveEvent(Path oldFilePath) {
		logger.trace("handleLocalMoveEvent - File: {}", getFilePath());
		acquireLockOnThis();
		if(isExecuting){

			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnLocalMove(oldFilePath);
			checkIfChanged();

		} else {
			updateTimestamp();
			if(oldFilePath.equals(getFilePath())){
				logger.trace("File {}:Move to same location due to update!", getFilePath());
				
//				eventManager.getDeletedFileComponents().gremove(oldFilePath.toString());
//				return;
			}
			currentState = currentState.handleLocalMove(oldFilePath);
			nextState = currentState.getDefaultState();
		}
		releaseLockOnThis();
	}
	
	public void handleRemoteUpdateEvent() {
		logger.trace("handleRemoteUpdateEvent - File: {}", getFilePath());
		acquireLockOnThis();
		if(isExecuting){

			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnRemoteUpdate();
			checkIfChanged();

		} else {
			updateTimestamp();
			currentState = currentState.handleRemoteUpdate();
			nextState = currentState.getDefaultState();
		}
		releaseLockOnThis();
	}
	
	public void handleRemoteDeleteEvent() {	
		logger.trace("handleRemoteDeleteEvent - File: {}", getFilePath());
		
		if(getFile().isFolder()){
			logger.trace("File {}: is a folder", getFile().getPath());
			FolderComposite folder = (FolderComposite)getFile();
			Map<String, FileComponent> children = folder.getChildren();
			
//			Vector<FileComponent> children = new Vector<FileComponent>(folder.getChildren().values());
			for(Map.Entry<String, FileComponent> childEntry : children.entrySet()){
				FileComponent child = childEntry.getValue();
				logger.trace("Child {}: handleLocalHardDelete", getFile().getPath());
				child.getAction().handleRemoteDeleteEvent();
			}
		}
		
		acquireLockOnThis();
		if(isExecuting){

			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnRemoteDelete();
			checkIfChanged();

		} else {
			updateTimestamp();
			currentState = currentState.handleRemoteDelete();
			nextState = currentState.getDefaultState();
		}
		releaseLockOnThis();
	}
	
	public void handleRemoteCreateEvent() {
		logger.trace("handleRemoteCreateEvent - File: {}", getFilePath());
		acquireLockOnThis();
		if(isExecuting){

			logger.trace("Event occured for {} while executing.", getFilePath());
			
			nextState = nextState.changeStateOnRemoteCreate();
			checkIfChanged();

		} else {
			updateTimestamp();
			currentState = currentState.handleRemoteCreate();
			nextState = currentState.getDefaultState();
		}
		releaseLockOnThis();
	}
	
	private void checkIfChanged() {
		if(!(nextState instanceof EstablishedState)){
			logger.trace("File {}: Next state is of type {}, keep track of change", getFilePath(), nextState.getClass());
			changedWhileExecuted = true;
		} else {
			logger.trace("File {}: Next state is of type {}, no change detected", getFilePath(), nextState.getClass());
		}
	}

	public void handleRemoteMoveEvent(Path path) {
		logger.trace("handleRemoteMoveEvent - File: {}", getFilePath());
		acquireLockOnThis();
		if(isExecuting){

			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnRemoteMove(path);
			checkIfChanged();

		} else {
			logger.trace("Currentstate: {} {}", getFilePath(), getCurrentState().getClass());
			updateTimestamp();
			currentState = currentState.handleRemoteMove(path);
			nextState = currentState.getDefaultState();
		}		
		releaseLockOnThis();
	}


	/**
	 * Each state is able to execute an action as soon the state is considered as stable. 
	 * The action itself depends on the current state (e.g. add file, delete file, etc.)
	 * @return 
	 * @throws NoSessionException
	 * @throws NoPeerConnectionException
	 * @throws IllegalFileLocation
	 * @throws InvalidProcessStateException 
	 */
	public ExecutionHandle execute(FileManager fileManager) throws NoSessionException,
			NoPeerConnectionException, InvalidProcessStateException {
		// this may be async, i.e. do not wait on completion of the process
		// maybe return the IProcessComponent object such that the
		// executor can be aware of the status (completion of task etc)
		ExecutionHandle ehandle = null;
		try{
			setIsExecuting(true);
			executionAttempts++;
			ehandle = currentState.execute(fileManager);
		} catch (Throwable t){
			// FIXME: Why catch throwable? Why here? Would this block an execution slot?
			logger.error("onLocalFileModified: Catched a throwable of type {} with message {}", t.getClass().toString(),  t.getMessage());
			for(int i = 0; i < t.getStackTrace().length; i++){
				StackTraceElement curr = t.getStackTrace()[i];
				logger.error("{} : {} ", curr.getClassName(), curr.getMethodName());
				logger.error("{} : {} ", curr.getFileName(), curr.getLineNumber());
			}
		}
		
		return ehandle;
	}
	
	public Path getFilePath(){
		return file.getPath();
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	/**
	 * @return current state object
	 */
	public AbstractActionState getCurrentState() { 
		logger.trace("Current state of {} is {}", getFilePath(), currentState.getClass());
		return currentState;
	}
	
	public synchronized void addEventListener(IActionEventListener listener) {
		eventListeners.add(listener);
	}
	
	public Set<IActionEventListener> getEventListener() {
		return eventListeners;
	}

	public int getExecutionAttempts() {
		return executionAttempts;
	}

//	public void putFile(String string, FileComponent file) {
//		eventManager.getFileTree().putComponent(string, file);
//	}

	@Override
	public void onSucceed() {
		setIsExecuting(false);
		logger.trace("onSucceed: File {}. Switch state from {} to {}", getFilePath(), currentState.getClass(), nextState.getClass());
		currentState = nextState;
		nextState = nextState.getDefaultState();
		changedWhileExecuted = false;
	}

	@Override
	public AbstractActionState getNextState() {
		// TODO Auto-generated method stub
		return nextState;
	}

	@Override
	public boolean isExecuting() {
		return isExecuting;
	}
	
	private void setIsExecuting(boolean isExecuting){
		this.isExecuting = isExecuting;
	}

	@Override
	public boolean getChangedWhileExecuted() {
		// TODO Auto-generated method stub
		return changedWhileExecuted;
	}

	@Override
	public void onFailed() {
		// TODO Auto-generated method stub
		setIsExecuting(false);
		isExecuting = false;
	}

	@Override
	public Lock getLock() {
		// TODO Auto-generated method stub
		return lock;
	}

	@Override
	public void handleRecoverEvent(int versionToRecover) {
		logger.trace("handleRecoverEvent - File: {}", getFilePath());
		acquireLockOnThis();
		if(isExecuting){

			logger.trace("Event occured for {} while executing.", getFilePath());
//			changedWhileExecuted = true;
			nextState = nextState.changeStateOnLocalRecover(versionToRecover);
			checkIfChanged();

		} else {
			updateTimestamp();
			currentState = currentState.handleLocalRecover(versionToRecover);
			nextState = currentState.getDefaultState();
		}
		releaseLockOnThis();
	}

//	@Override
//	public void handleException(ProcessExceptionVisitor visitor) {
//		// TODO Auto-generated method stub
//		visitor.visit(this);
//		System.out.println("Action handle exception");
//	}
}
