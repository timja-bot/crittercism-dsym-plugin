package org.jenkinsci.plugins.crittercism_dsym;

import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class RemoteRecorder implements Callable<Object, Throwable>, Serializable {
    final private String remoteWorkspace;
    final private CrittercismUploader.UploadRequest uploadRequest;
    final private BuildListener listener;

    public RemoteRecorder(String remoteWorkspace, CrittercismUploader.UploadRequest uploadRequest, BuildListener listener) {
        this.remoteWorkspace = remoteWorkspace;
        this.uploadRequest = uploadRequest;
        this.listener = listener;
    }

    public Object call() throws Throwable {
        CrittercismUploader uploader = new CrittercismUploader();
        return uploadWith(uploader);
    }

    List<Map> uploadWith(CrittercismUploader uploader) throws Throwable {
        List<Map> results = new ArrayList<Map>();

            HashMap result = new HashMap();

            CrittercismUploader.UploadRequest ur = CrittercismUploader.UploadRequest.copy(uploadRequest);
            ur.dsymFile = identifyDsym(ur.dsymPath);
            listener.getLogger().println("DSYM: " + ur.dsymFile);
            uploader.upload(ur, listener);

            results.add(result);
        return results;
    }


    /* if a specified filePath is specified, return it, otherwise find in the workspace the DSYM matching the specified ipa file name */
    private File identifyDsym(String filePath) {
        File dsymFile;
        if (filePath != null && !filePath.trim().isEmpty()) {
            dsymFile = findAbsoluteOrRelativeFile(filePath);
            if (dsymFile == null)
                throw new IllegalArgumentException("Couldn't find file " + filePath + " in workspace " + remoteWorkspace);

        } else {
            //String fileName = FilenameUtils.removeExtension(ipaName);
            File f = new File(filePath);
            if (f.exists()) {
                dsymFile = f;
            } else {
                f = new File(filePath);
                if (f.exists()) {
                    dsymFile = f;
                } else
                    dsymFile = null;
            }
        }
        return dsymFile;
    }

    /*
      * Finds a file that is absolute or relative to either the current direectory or the remoteWorkspace
     */
    private File findAbsoluteOrRelativeFile(String path) {
        File f = new File(path);
        if (f.exists())
            return f;
        f = new File(remoteWorkspace, path);
        if (f.exists())
            return f;
        return null;
    }
}