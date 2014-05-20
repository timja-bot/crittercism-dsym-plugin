package org.jenkinsci.plugins.crittercism_dsym;
import hudson.model.BuildListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.parser.JSONParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A testflight uploader
 */
public class CrittercismUploader implements Serializable {
    static interface Logger {
        void logDebug(String message);
    }

    static class UploadRequest implements Serializable {
        String dsymPath;
        String apiKey;
        String appId;
        File dsymFile;

        public String toString() {
            return new ToStringBuilder(this)
                    .append("dsymPath", dsymPath)
                    .append("apiKey", "********")
                    .append("appId", "********")
                    .append("dsymFile", dsymFile)
                    .toString();
        }

        static UploadRequest copy(UploadRequest r) {
            UploadRequest r2 = new UploadRequest();
            r2.dsymPath = r.dsymPath;
            r2.apiKey = r.apiKey;
            r2.appId = r.appId;
            r2.dsymFile = r.dsymFile;

            return r2;
        }
    }

    private Logger logger = null;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Map upload(UploadRequest ur, BuildListener listener) throws IOException, org.json.simple.parser.ParseException {
        HttpClient httpClient = new DefaultHttpClient();

        HttpPost httpPost = new HttpPost("https://app.crittercism.com/api_beta/dsym/" + ur.appId);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("key", new StringBody(ur.apiKey));

        if (ur.dsymFile != null) {
            FileBody dsymFileBody = new FileBody(ur.dsymFile);
            entity.addPart("dsym", dsymFileBody);
        }

        httpPost.setEntity(entity);

        logDebug("POST Request: " + ur);

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity resEntity = response.getEntity();

        InputStream is = resEntity.getContent();

        // Improved error handling.
        int statusCode = response.getStatusLine().getStatusCode();
        listener.getLogger().println(statusCode);
        if (statusCode != 200) {
            String responseBody = new Scanner(is).useDelimiter("\\A").next();

            listener.getLogger().println(responseBody);
            listener.getLogger().println(response);

            throw new UploadException(statusCode, responseBody, response);
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        String json = writer.toString();

        logDebug("POST Answer: " + json);

        JSONParser parser = new JSONParser();

        return null;
    }

    private void logDebug(String message) {
        if (logger != null) {
            logger.logDebug(message);
        }
    }
}