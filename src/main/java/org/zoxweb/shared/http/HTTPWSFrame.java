package org.zoxweb.shared.http;

import org.zoxweb.shared.protocol.BytesArray;
import org.zoxweb.shared.protocol.DataBufferController;

public class HTTPWSFrame
    implements HTTPWSProto.WSFrameInfo
{


    /**
     * @return true if the frame is the end of the message
     */
    @Override
    public boolean isFin() {
        return false;
    }

    /**
     * @return false now
     */
    @Override
    public boolean isRSV1() {
        return false;
    }

    /**
     * @return false now
     */
    @Override
    public boolean isRSV2() {
        return false;
    }

    /**
     * @return false now
     */
    @Override
    public boolean isRSV3() {
        return false;
    }

    /**
     * The frame type op code text, binary, close, ping, pong
     *
     * @return op code enum
     */
    @Override
    public HTTPWSProto.OP_CODE opCode() {
        return null;
    }

    /**
     * If true the payload is masked and maskingKey must return byte[4]
     *
     * @return true is the payload is masked usually a client request
     */
    @Override
    public boolean isMasked() {
        return false;
    }

    /**
     * If is masked is true return byte[4] the masking key that need to be xored with the payload data
     *
     * @return byte[4] or byte[0] array
     */
    @Override
    public byte[] maskingKey() {
        return new byte[0];
    }

    /**
     * @return the payload size we support 125 bytes or 16bit size 32bit will not be supported
     */
    @Override
    public int payloadSize() {
        return 0;
    }

    /**
     * @return the payload data
     */
    @Override
    public BytesArray payload() {
        return null;
    }

    /**
     * BytesArray Object representing the whole frame
     * @return DataBufferController object of the whole frame
     */
    @Override
    public DataBufferController rawData()
    {
        return null;
    }
    @Override
    public int rawDataOffset()
    {
        return -1;
    }
}
