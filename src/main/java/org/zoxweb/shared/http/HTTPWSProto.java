package org.zoxweb.shared.http;

import org.zoxweb.shared.protocol.BytesArray;
import org.zoxweb.shared.protocol.DataBufferController;

public class HTTPWSProto
{
    private HTTPWSProto(){}
    public static final String WEB_SOCKET_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public enum OP_CODE
    {
        TEXT((byte) 0x1),
        BINARY((byte) 0x2),
        CLOSE((byte) 0x8),
        PING((byte) 0x9),
        PONG((byte) 0xA),

        ;
        private final byte opCode;
        OP_CODE(byte code)
        {
            opCode = code;
        }
        public byte opCode()
        {
            return opCode;
        }
    }

    public interface WSFrameInfo
    {

        // WebSocket protocol frame info

        /**
         * @return true if the frame is the end of the message
         */
        boolean isFin();

        /**
         * @return false now
         */
        boolean isRSV1();
        /**
         * @return false now
         */
        boolean isRSV2();
        /**
         * @return false now
         */
        boolean isRSV3();

        /**
         * The frame type op code text, binary, close, ping, pong
         * @return op code enum
         */
        HTTPWSProto.OP_CODE opCode();

        /**
         * If true the payload is masked and maskingKey must return byte[4]
         * @return true is the payload is masked usually a client request
         */
        boolean isMasked();

        /**
         * If is masked is true return byte[4] the masking key that need to be xored with the payload data
         * @return byte[4] or byte[0] array
         */
        byte[] maskingKey();

        /**
         * @return the payload size we support 125 bytes or 16bit size 32bit will not be supported
         */
        int payloadSize();

        /**
         * @return the payload data
         */
        BytesArray payload();

        // control info

        /**
         * BytesArray Object representing the whole frame
         * @return DataBufferController object of the whole frame
         */
        DataBufferController rawData();

        int rawDataOffset();


    }

}
