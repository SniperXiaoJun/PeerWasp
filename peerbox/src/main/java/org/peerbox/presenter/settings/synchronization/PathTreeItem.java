package org.peerbox.presenter.settings.synchronization;

import java.nio.file.Path;

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.peerbox.watchservice.IFileEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class PathTreeItem extends CheckBoxTreeItem<PathItem> {
	
	private static final Logger logger = LoggerFactory.getLogger(PathTreeItem.class);
	private boolean isFile;
	
    private IFileEventManager fileEventManager;

    public PathTreeItem(Path path){
    	this(path, null, false);
    }
    
    public PathTreeItem(Path path, ImageView view){
    	this(path, view, false);
    }
    
    public PathTreeItem(Path path, ImageView view, boolean isSynched){
    	super(new PathItem(path));
    	setGraphic(view);
    	setSelected(isSynched);
    }
    
    public PathTreeItem(Path path, ImageView view, boolean isSynched, boolean isFile){
    	super(new PathItem(path));
    	setGraphic(view);
    	setSelected(isSynched);
    	setIsFile(isFile);
    }
    
    
    public PathTreeItem(Path path, boolean isSelected, boolean isFile){
    	super(new PathItem(path));
      	this.isFile = isFile;
      	setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/index_progress.jpg"))));
    }
    
    public boolean isFile(){
    	return isFile;
    }
    
    private void setIsFile(boolean isFile){
    	this.isFile = isFile;
    }
    
    public boolean isFolder(){
    	return !isFile;
    }
}