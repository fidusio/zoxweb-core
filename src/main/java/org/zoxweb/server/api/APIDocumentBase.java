package org.zoxweb.server.api;

import org.zoxweb.server.util.LockQueue;
import org.zoxweb.shared.api.APIDocumentStore;
import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.api.APIFileInfoMap;
import org.zoxweb.shared.data.DataConst.DocumentStatus;
import org.zoxweb.shared.security.AccessException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;

@SuppressWarnings("serial")
abstract public class APIDocumentBase<P, S>
        extends APIServiceProviderBase<P, S>
        implements APIDocumentStore<P, S> {


    private final LockQueue writeQueue;
    private final LockQueue deleteQueue;


    protected APIDocumentBase(int writeQueueSize, int deleteQueueSize) {
        super();
        if (writeQueueSize > 0) {
            writeQueue = new LockQueue(writeQueueSize);
        } else {
            writeQueue = null;
        }

        if (deleteQueueSize > 0) {
            deleteQueue = new LockQueue(deleteQueueSize);
        } else {
            deleteQueue = null;
        }
    }


    /**
     * This method creates a file.
     * @param folderID
     * @param file
     * @param is
     * @param closeStream
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws AccessException
     * @throws APIException
     */
    public APIFileInfoMap createFile(String folderID, APIFileInfoMap file, InputStream is, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {


        Lock lock = null;
        try {
            pendingCalls.incrementAndGet();
            if (writeQueue != null)
                lock = writeQueue.dequeueLock();

            return internalCreateFile(folderID, file, is, closeStream);
        } finally {
            if (lock != null)
                writeQueue.queueLock(lock);

            touch();
            pendingCalls.decrementAndGet();
        }
    }

    protected abstract APIFileInfoMap internalCreateFile(String folderID, APIFileInfoMap file, InputStream is, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;


    /**
     *
     * Create a folder
     * @param folderFullPath
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws AccessException
     */
    public APIFileInfoMap createFolder(String folderFullPath)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {
        try {

            pendingCalls.incrementAndGet();
            return internalCreateFolder(folderFullPath);
        } finally {
            touch();
            pendingCalls.decrementAndGet();
        }
    }


    protected abstract APIFileInfoMap internalCreateFolder(String folderFullPath)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * This method reads a file.
     * @param map
     * @param os
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws NullPointerException
     */
    public APIFileInfoMap readFile(APIFileInfoMap map, OutputStream os, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {
        try {

            pendingCalls.incrementAndGet();
            if (map.getOriginalFileInfo().getStatus() == DocumentStatus.AVAILABLE) {
                return internalReadFile(map, os, closeStream);
            } else {
                throw new APIException(map.getOriginalFileInfo().getName() + " is not available at the moment.");
            }

        } finally {
            touch();
            pendingCalls.decrementAndGet();
        }
    }


    protected abstract APIFileInfoMap internalReadFile(APIFileInfoMap map, OutputStream os, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * This method updates a file.
     * @param map
     * @param is
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws NullPointerException
     */
    public synchronized APIFileInfoMap updateFile(APIFileInfoMap map, InputStream is, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {
        try {

            pendingCalls.incrementAndGet();
            if (map.getOriginalFileInfo().getStatus() != DocumentStatus.AVAILABLE) {


                // 2 ways to perform update
                // delete and add
                // side effect loose previous versions
                // user quota will not grow
				
				/*	internalDeleteFile(map);
					return internalCreateFile(map.getOriginalFileInfo().getResourceLocation(), map, is, closeStream);*/


                // update
                // side effect the user quota will grow
                return internalUpdateFile(map, is, closeStream);
            } else {
                throw new APIException(map.getOriginalFileInfo().getName() + " is not available at the moment.");
            }

        } finally {
            touch();
            pendingCalls.decrementAndGet();
        }
    }


    protected abstract APIFileInfoMap internalUpdateFile(APIFileInfoMap map, InputStream is, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * This method deletes a file.
     * @param map
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws NullPointerException
     */
    public void deleteFile(APIFileInfoMap map)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {
        Lock lock = null;
        try {

            pendingCalls.incrementAndGet();
            if (deleteQueue != null)
                lock = deleteQueue.dequeueLock();
            internalDeleteFile(map);
        } finally {
            if (lock != null)
                deleteQueue.queueLock(lock);

            touch();
            pendingCalls.decrementAndGet();
        }
    }


    protected abstract void internalDeleteFile(APIFileInfoMap map)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

}
