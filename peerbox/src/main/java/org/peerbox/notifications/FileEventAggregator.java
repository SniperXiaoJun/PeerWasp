package org.peerbox.notifications;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.engio.mbassy.listener.Handler;

import org.peerbox.app.manager.file.messages.RemoteFileAddedMessage;
import org.peerbox.app.manager.file.messages.RemoteFileDeletedMessage;
import org.peerbox.app.manager.file.messages.RemoteFileMovedMessage;
import org.peerbox.app.manager.file.messages.RemoteFileUpdatedMessage;
import org.peerbox.events.IMessageListener;
import org.peerbox.events.MessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;


public class FileEventAggregator implements IMessageListener {

	private static final Logger logger = LoggerFactory.getLogger(FileEventAggregator.class);
	protected static final int AGGREGATION_TIMESPAN = 10000;

	private final MessageBus messageBus;

	private List<Path> addedFiles;
	private List<Path> movedFiles;
	private List<Path> updatedFiles;
	private List<Path> deletedFiles;

	private volatile Timer timer;

	@Inject
	public FileEventAggregator(MessageBus messageBus) {
		this.messageBus = messageBus;
		addedFiles = new ArrayList<Path>();
		movedFiles = new ArrayList<Path>();
		updatedFiles = new ArrayList<Path>();
		deletedFiles = new ArrayList<Path>();
	}

	@Handler
	public void onFileAdded(RemoteFileAddedMessage message) {
		logger.trace("onFileAdded.");
		Path path = message.getFile().getPath();
		addedFiles.add(path);
		scheduleNotification();
	}

	@Handler
	public void onFileMoved(RemoteFileMovedMessage message) {
		logger.trace("onFileMoved.");
		Path path = message.getFile().getPath();;
		movedFiles.add(path);
		scheduleNotification();
	}

	@Handler
	public void onFileUpdated(RemoteFileUpdatedMessage message) {
		logger.trace("onFileUpdated.");
		Path path = message.getFile().getPath();
		updatedFiles.add(path);
		scheduleNotification();
	}

	@Handler
	public void onFileDeleted(RemoteFileDeletedMessage message) {
		logger.trace("onFileDeleted.");
		Path path = message.getFile().getPath();
		deletedFiles.add(path);
		scheduleNotification();
	}

	private void scheduleNotification() {
		if(timer == null) {
			timer = new Timer(getClass().getName());
			timer.schedule(new TimerTask() {

				List<Path> added = null;
				List<Path> updated = null;
				List<Path> deleted = null;
				List<Path> moved = null;

				@Override
				public void run() {
					timer = null;

					synchronized (addedFiles) {
						added = addedFiles;
						addedFiles = new ArrayList<Path>();
					}
					synchronized (updatedFiles) {
						updated = updatedFiles;
						updatedFiles = new ArrayList<Path>();
					}
					synchronized (deletedFiles) {
						deleted = deletedFiles;
						deletedFiles = new ArrayList<Path>();
					}
					synchronized (movedFiles){
						moved = movedFiles;
						movedFiles = new ArrayList<Path>();
					}

					AggregatedFileEventStatus event = new AggregatedFileEventStatus(added.size(),
							updated.size(), deleted.size(), moved.size());

					messageBus.publish(event);
				}
			}, AGGREGATION_TIMESPAN);
		}
		// else: already scheduled...
	}

}
