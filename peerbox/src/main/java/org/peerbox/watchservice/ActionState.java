package org.peerbox.watchservice;

import java.nio.file.Path;

import org.hive2hive.core.exceptions.IllegalFileLocation;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;

/**
 * Interface for different states of implemented state pattern
 * 
 * @author winzenried
 *
 */
public interface ActionState {
	
	public ActionState handleCreateEvent();
	public ActionState handleDeleteEvent();
	public ActionState handleModifyEvent();
	public ActionState handleMoveEvent(Path filePath);
	public void execute(Path filePath) throws NoSessionException, NoPeerConnectionException, IllegalFileLocation;
}
