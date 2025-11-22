package edu.asu.jmars.layer.map2.custom;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

public class UploadFileRunner implements Runnable {
    
    private List<CustomMap> uploadFiles = new ArrayList<CustomMap>();
    private List<CustomMap> uploadFilesInWaiting = new ArrayList<CustomMap>();
    
    public UploadFileRunner(List<CustomMap> uFiles) {
        this.uploadFilesInWaiting.addAll(uFiles);
    }
    public UploadFileRunner(CustomMap file) {
        this.uploadFilesInWaiting.add(file);
    }
    public void addUploadFiles(List<CustomMap> uFiles) {
        this.uploadFilesInWaiting.addAll(uFiles);
    }
    public void addUploadFile(CustomMap file) {
        this.uploadFilesInWaiting.add(file);
    }
    @Override
    public void run() {
        try {
            while(this.uploadFilesInWaiting.size() > 0) {
                this.uploadFiles.addAll(this.uploadFilesInWaiting);
                this.uploadFilesInWaiting.clear();
                for (int i=this.uploadFiles.size()-1; i>=0; i--) {
                    CustomMap file = this.uploadFiles.get(i);
                    if (!file.hasStartedUpload() && !file.isCanceled()) {
                        if(file.getSelectedUploadProcess() == UploadFile.PROCESSING_OPTION_UPLOAD) {
                            CM_Manager.updateStatusOnEDThread(i);
                            CustomMapBackendInterface.uploadAndProcessMap(file);
                            CM_Manager.updateStatusOnEDThread(i);
                        } else if (file.getSelectedUploadProcess() == UploadFile.PROCESSING_OPTION_MANUAL) {
                            CM_Manager.updateStatusOnEDThread(i);
                            CustomMapBackendInterface.uploadAndProcessMap(file);
                            CM_Manager.updateStatusOnEDThread(i);
                        } else {//prompt
                            CustomMapBackendInterface.insertCustomMapRecord(file);
                            CM_Manager.updateStatusOnEDThread(i);
                            CustomMapBackendInterface.uploadCustomMap(file);
                            if (file.getCustomMapId() != null && !file.isErrorCondition()) {
                                file.setStatus(UploadFile.STATUS_READING_HEADER);
                                CM_Manager.updateStatusOnEDThread(i);
                                CustomMapBackendInterface.startHeaderProcessing(file);
                            }
                        }
                    }
                    this.uploadFiles.remove(i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
