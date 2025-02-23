package org.zoxweb.shared.protocol;

import org.zoxweb.shared.util.BytesArray;
import org.zoxweb.shared.util.DataBufferController;
import org.zoxweb.shared.util.SharedStringUtil;

public class HTTPWSProto
{
    private HTTPWSProto(){}
    public static final String WEB_SOCKET_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public enum OpCode
    {
        TEXT((byte) 0x1),
        BINARY((byte) 0x2),
        CLOSE((byte) 0x8),
        PING((byte) 0x9),
        PONG((byte) 0xA),

        ;
        private final byte opCode;
        OpCode(byte code)
        {
            opCode = code;
        }
        public byte opCode()
        {
            return opCode;
        }


        public static OpCode decode(int val)
        {
            val = val & 0x0F;
            for(OpCode oc : OpCode.values())
            {
                if (val == oc.opCode)
                    return oc;
            }

            return null;
        }
    }

    public enum WSFrameField
    {
        FIN("FIN", "Final frame bit true if last frame false more to come", 0, 0, 1),
        RSV1("RSV1", "RSV1 bit", 0, 1, 1),
        RSV2("RSV2", "RSV2 bit", 0, 2, 1),
        RSV3("RSV1", "RSV1 bit", 0, 3, 1),
        OP_CODE("OPCODE", "The message type code, text, binary, close, ping and pong", 0, 4, 4),
        MASK_BIT("MaskBit", "The masking bit if true the masking key must be used to xor the payload", 1, 0, 1),
        DATA_LENGTH("DataLength", "The of the data length up to 125 bytes with the byte, if 126 the next 16 bits are the length, 127 NOT SUPPORTED", 1, 1, 7),
        MASKING_KEY("MaskingKey", "4 bytes masking key if set usually by the client the server must xor the payload to read the data", -1, -1, 4),
        DATA("Data", "Data in bytes", -1, -1 ,-1 ),
        ;
        public final String name;
        public final String description;
        public final int byteIndex;
        public final int bitIndex;
        public final int length;

        WSFrameField(String name, String description, int byteIndex, int bitIndex, int length)
        {
            this.name = name;
            this.description = description;
            this.byteIndex = byteIndex;
            this.bitIndex = bitIndex;
            this.length = length;

        }
    }


    public static void formatFrame(DataBufferController dataBuffer, boolean fin, OpCode opcode, byte[] maskingKey, String data)
    {
        formatFrame(dataBuffer, fin, opcode, maskingKey, data != null ? SharedStringUtil.toBytes(data) : null);
    }
    public static void formatFrame(DataBufferController dataBuffer, boolean fin, OpCode opcode, byte[] maskingKey, byte[] data)
    {
        if(maskingKey != null)
        {
            if (maskingKey.length != 4)
                throw new IllegalArgumentException("Invalid payload mask length " + maskingKey.length);
        }

        // First byte: FIN flag and opcode
        int firstByte = 0;
        if (fin) {
            firstByte |= 0x80;  // set FIN bit
        }
        firstByte |= (opcode.opCode() & 0x0F); // lower 4 bits for opcode
        dataBuffer.write(firstByte);

        // Second byte: no mask (0) and payload length
        int payloadLength = (data != null) ? data.length : 0;

        if (payloadLength <= 125)
        {
            dataBuffer.write(maskingKey != null ? payloadLength | 0x80 : payloadLength);
        }
        else if (payloadLength <= 0xFFFF) { // fits in 16 bits
            dataBuffer.write(maskingKey != null ? 126 | 0x80 : 126);
            dataBuffer.write((payloadLength >> 8) & 0xFF);
            dataBuffer.write(payloadLength & 0xFF);
        }
        else {
            dataBuffer.write(maskingKey != null ? 127 | 0x80 : 127);
            // Write the payload length as an 8-byte long (big-endian)
            for (int i = 7; i >= 0; i--) {
                dataBuffer.write((int) (payloadLength >> (8 * i)) & 0xFF);
            }
        }
        if(maskingKey != null)
        {
            dataBuffer.write(maskingKey);
        }


        // Write the payload data directly (server-to-client frames are not masked)
        if (data != null)
        {
            if (maskingKey != null)
            {
                for(int i = 0; i < data.length; i++)
                {
                    dataBuffer.write(data[i] ^ maskingKey[i % 4] );
                }
            }
            else
                dataBuffer.write(data);
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
        OpCode opCode();

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
        int dataLength();

        /**
         * @return the payload data
         */
        BytesArray data();

        // control info

        /**
         * BytesArray Object representing the whole frame
         * @return DataBufferController object of the whole frame
         */
        DataBufferController rawData();

        int rawDataOffset();

        /**
         * @return the -1 if not reached yet, otherwise the end of the frame offset exclusive
         */
        int endFrameOffset();

        /**
         *
         * @param field looking for
         * @return relative starting byte index of field
         */
        int byteIndex(WSFrameField field);

        /**
         * @return status
         */
        MessageStatus status();

        /**
         * @return the expected size of frame including header and data, -1 buffer incomplete
         */
        int frameSize();
    }

}
