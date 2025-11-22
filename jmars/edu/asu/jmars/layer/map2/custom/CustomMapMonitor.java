package edu.asu.jmars.layer.map2.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

public class CustomMapMonitor implements Runnable {
    
    List<CustomMap> uploadFiles = null;
    
//    public void deleteInProgressFile(CustomMap file) {
//        this.filesToDelete.add(file);
//    }
    public void setUploadFiles(List<CustomMap> files) {
        this.uploadFiles = files;
    }
    @Override
    public void run() {
        try {
            boolean continueFlag = true;
            int awaitingInputCount = 0;
            ArrayList<CustomMap> completedFiles = new ArrayList<CustomMap>();
            while(continueFlag) {
                final CM_Manager cmm = CM_Manager.getInstance();
                boolean success = CustomMapBackendInterface.checkMapProcessingStatus(this.uploadFiles);
                if (success) {
                    boolean endThread = true;
                    
                    
                    if (this.uploadFiles.size() == 0) {
                        continueFlag = false;
                    }
                    final ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
                    for (int i=0; i<this.uploadFiles.size(); i++) {
                        CustomMap file = this.uploadFiles.get(i);
                        //the following if statement is to find files that need to be moved from the in progress table to the completed table
                        if (UploadFile.STATUS_COMPLETE.equalsIgnoreCase(file.getStatus())
                             || UploadFile.STATUS_ERROR.equalsIgnoreCase(file.getStatus())
                             || UploadFile.STATUS_CANCELED.equalsIgnoreCase(file.getStatus())) {
                            toRemove.add(file);
                            if (CustomMap.STATUS_COMPLETE.equalsIgnoreCase(file.getStatus())) {
                                completedFiles.add(file);//to update capabilities
                            }
                        }
                        //the following if statement is to find files that are in some sort of state that requires us to keep the monitor thread alive
                        if (!UploadFile.STATUS_AWAITING_USER_INPUT.equalsIgnoreCase(file.getStatus())
                             && !UploadFile.STATUS_COMPLETE.equalsIgnoreCase(file.getStatus())
                             && !UploadFile.STATUS_ERROR.equalsIgnoreCase(file.getStatus())
                             && !UploadFile.STATUS_CANCELED.equalsIgnoreCase(file.getStatus())
//                             && !UploadFile.STATUS_NOT_STARTED.equalsIgnoreCase(file.getStatus())
                             ) {
                            
                            if (!(UploadFile.STATUS_IN_PROGRESS.equalsIgnoreCase(file.getStatus()) && UploadFile.STAGE_HEADER_ANALYZED.equalsIgnoreCase(file.getStage()))) {
                                endThread = false;
                            } 
                            
                        }
                        //if the status of this file is "in progress", check if the header has been analyzed
                        if (UploadFile.STATUS_IN_PROGRESS.equalsIgnoreCase(file.getStatus())) {
                            if (UploadFile.STAGE_HEADER_ANALYZED.equalsIgnoreCase(file.getStage())) {
                                //if we are at the point where we need feedback on the header, update the status and get the header information
                                // if they selected prompt, set the status to awaiting input and get header information
                                if (file.getSelectedUploadProcess() == -1 || file.getSelectedUploadProcess() == UploadFile.PROCESSING_OPTION_PROMPT) {
                                    file.setStatus(UploadFile.STATUS_AWAITING_USER_INPUT);
                                    CustomMapBackendInterface.getHeaderInformation(file);
                                } else {
                                    //this is the case when it happens to be in progress/5 but is processing all the way through
                                    //except, we need to check the case when the header information was not valid
                                    if (file.isValidHeaderFlag()) {
                                        endThread = false;
                                    } else {
                                        file.setStatus(UploadFile.STATUS_AWAITING_USER_INPUT);
                                    }
                                }
                            }
                        } 
                        
                        //we get here for files with a status of awaiting user input
                        if (UploadFile.STATUS_AWAITING_USER_INPUT.equalsIgnoreCase(file.getStatus())) {
                        	if (awaitingInputCount < 3) {
                        		//this is mainly for when we have one file being uploaded and happened to land on stage 5. 
                        		//we want the thread to continue to process unless we are sure that this map is staying at stage 5 and is truly waiting for user input
                        		endThread = false;
                        		awaitingInputCount++;
                        	}
                        }
                        //update the status
                        final AtomicInteger atomicI = new AtomicInteger(i);
                        SwingUtilities.invokeAndWait(new Runnable() {

                            @Override
                            public void run() {
                                cmm.updateProgressStatus(atomicI.get());//update the status for this row;
                            }
                
                        });
                        
                    }
                    //update tables in the gui on the EDT
                    if (toRemove.size() > 0) {
                        SwingUtilities.invokeAndWait(new Runnable() {
                           
                            @Override
                            public void run() {
                                cmm.addToCompleted(toRemove);
                            }
                        });
                        this.uploadFiles.removeAll(toRemove);
                    }
                    //update capabilities if necessary
                    boolean updateCapabilities = false;
                    for (CustomMap file : completedFiles) {
                        if (!file.checkCapabilitiesRun()) {
                            updateCapabilities = true;
                            file.capabilitiesHaveBeenRun();
                        }
                    }
                    completedFiles.clear();
                    if (updateCapabilities) {
                        CustomMapBackendInterface.refreshCapabilities();
                    }
                    cmm.addNewlyCompletedFilesToSearch();
                    
                    //end update capabilities
                    if (endThread) {
                        continueFlag = false;
                    } else {
                        Thread.sleep(2000);
                    }
                } else {
                    return;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}
