package org.peerbox.watchservice.filetree.composite;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.peerbox.watchservice.Action;
import org.peerbox.watchservice.IAction;
import org.peerbox.watchservice.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLeaf extends AbstractFileComponent{
	private IAction action;
	private Path path;
	private Path fileName;
	private String contentHash;
	private FolderComposite parent;
	private boolean isSynchronized = false;
	
	private static final Logger logger = LoggerFactory.getLogger(FileLeaf.class);
	
	public FileLeaf(Path path){
		this.path = path;
		this.fileName = path.getFileName();
		this.action = new Action();
		this.contentHash = "";
		updateContentHash();
	}
	
	public FileLeaf(Path path, boolean maintainContentHashes){
		this.path = path;
		this.fileName = path.getFileName();
		this.action = new Action();
		this.contentHash = "";
		if(maintainContentHashes){
			updateContentHash();
		}

	}

	@Override
	public IAction getAction() {
		return this.action;
	}

	@Override
	public void putComponent(String path, FileComponent component) {
		System.err.println("put on file not defined.");
	}

	@Override
	public FileComponent deleteComponent(String path) {
		System.err.println("delete on file not defined");
		return null;
	}

	@Override
	public FileComponent getComponent(String path) {
		logger.error("get on file not defined {} {}", this.path, path);
		return null;
	}
	
	@Override
	public String getContentHash() {
		return contentHash;
	}

	@Override
	public void bubbleContentHashUpdate() {
		bubbleContentHashUpdate(null);
	}
	

	@Override
	public void bubbleContentHashUpdate(String contentHash) {
		boolean hasChanged = updateContentHash(contentHash);
		if(hasChanged){
			getParent().bubbleContentHashUpdate();
		}
	}

	@Override
	public void setParent(FolderComposite parent) {
		this.parent = parent;
	}

	@Override
	public Path getPath() {
		return this.path;
	}
	

	public boolean updateContentHash() {
		return updateContentHash(null);
	}
	
	/**
	 * Computes and updates this FileLeafs contentHash property.
	 * @return true if the contentHash hash changed, false otherwise.
	 * @param newHash provided content hash. If this is null, the content hash is
	 * calculated on the fly. If this is not null, it is assumed to be the correct
	 * hash of the file's content at the time of the call.
	 */

	private boolean updateContentHash(String newHash) {
		if(newHash == null){
			newHash = PathUtils.computeFileContentHash(getPath());
		} 
		if(!contentHash.equals(newHash)){
			contentHash = newHash;
			
			return true;
		} else {
//			logger.debug("No content hash update: {}", contentHash);
			return false;
		}
	}

	@Override
	public FolderComposite getParent() {
		return this.parent;
	}

	@Override
	public boolean getActionIsUploaded() {
		return this.getAction().getIsUploaded();
	}

	@Override
	public void setActionIsUploaded(boolean isUploaded) {
		this.getAction().setIsUploaded(isUploaded);
	}
	
	@Override
	public void setParentPath(Path parentPath){	
		if(parentPath != null){
			this.path = Paths.get(new File(parentPath.toString(), fileName.toString()).getPath());
//			logger.debug("Set path to {}", path);
		}
		
	}
	
	@Override
	public void setPath(Path path){
		this.path = path;
		this.fileName = path.getFileName();
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean isReady() {
		if(parent.getActionIsUploaded()){
			return true;
		}
		return false;
	}

	@Override
	public String getStructureHash() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void setStructureHash(String hash) {
		logger.debug("setStructureHash(String) is not defined on FileLeaf.");
	}

	@Override
	public void propagatePathChangeToChildren() {
		return;
	}

	@Override
	public boolean getIsSynchronized() {
		// TODO Auto-generated method stub
		return isSynchronized;
	}
	
	@Override
	public void setIsSynchronized(boolean isSynchronized) {
		this.isSynchronized = isSynchronized;
	}

	@Override
	public void getSynchronizedChildrenPaths(Set<Path> synchronizedPaths) {
		return;
	}

}