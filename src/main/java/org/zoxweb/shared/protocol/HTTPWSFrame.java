package org.zoxweb.shared.protocol;

import org.zoxweb.shared.util.BytesArray;
import org.zoxweb.shared.util.DataBufferController;

public class HTTPWSFrame
    implements HTTPWSProto.WSFrameInfo
{

    private final int startOffset;
    private final DataBufferController dataBuffer;
    private int fullMessageSize = -1;
    private MessageStatus status = MessageStatus.UNPROCESSED;
    private BytesArray data = null;




    public HTTPWSFrame(DataBufferController dataBuffer, int startOffset)
    {
        this.dataBuffer = dataBuffer;
        this.startOffset = startOffset;
    }

    /**
     * @return true if the frame is the end of the message
     */
    @Override
    public boolean isFin()
    {
        return (dataBuffer.byteAt(startOffset + HTTPWSProto.WSFrameField.FIN.byteIndex) & 0x80) != 0;
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
    public HTTPWSProto.OpCode opCode()
    {

        return HTTPWSProto.OpCode.decode(dataBuffer.byteAt(startOffset + HTTPWSProto.WSFrameField.OP_CODE.byteIndex));
    }

    /**
     * If true the payload is masked and maskingKey must return byte[4]
     *
     * @return true is the payload is masked usually a client request
     */
    @Override
    public boolean isMasked() {
        return (dataBuffer.byteAt(startOffset + HTTPWSProto.WSFrameField.MASK_BIT.byteIndex) & 0x80) != 0;
    }

    /**
     * If is masked is true return byte[4] the masking key that need to be xored with the payload data
     *
     * @return byte[4] or null array
     */
    @Override
    public byte[] maskingKey()
    {
        byte[] maskingKey = null;
        if (isMasked())
        {
            int index = 0;
            if (dataLength() < 126)
                index = 2;
            else
                index = 4;

            maskingKey = new byte[4];
            for(int i = 0; i < maskingKey.length; i++)
                maskingKey[i] = dataBuffer.byteAt(startOffset + index + i);
        }

        return maskingKey;
    }

    /**
     * @return the payload size we support 125 bytes or 16bit size 32bit will not be supported
     */
    @Override
    public int dataLength()
    {
        int payloadLen = dataLengthByte();
        if (payloadLen == 126)
        {
            payloadLen = (dataBuffer.byteAt(startOffset + HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex + 1) << 8) |
                         (dataBuffer.byteAt(startOffset + HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex + 2) & 0xFF);
        }
        else if(payloadLen == 127)
             throw new UnsupportedOperationException("32 bit data length not supported, only 16 bit or < 126");
        return payloadLen;
    }

    private int dataLengthByte()
    {
        return dataBuffer.byteAt(startOffset + HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex)& 0x7F;
    }

    /**
     * @return the payload data
     */
    @Override
    public BytesArray data()
    {
        if (data == null)
        {
            synchronized (this)
            {
                if (data == null && frameSize() != -1) {
                    int dataIndex = byteIndex(HTTPWSProto.WSFrameField.DATA);
                    int dataLength = dataLength();
                    byte[] maskingKey = maskingKey();
                    if (maskingKey != null) {
                        for (int i = 0; i < dataLength; i++) {
                            byte b = (byte) ((dataBuffer.byteAt(startOffset + dataIndex + i) ^ maskingKey[i % 4]));
                            dataBuffer.writeAt(startOffset + dataIndex + i, b);
                        }
                    }
                    data = new BytesArray(dataBuffer.getInternalBuffer(), dataIndex + startOffset, dataLength);
                }
            }
        }

        return data;
    }

    /**
     * BytesArray Object representing the whole frame
     * @return DataBufferController object of the whole frame
     */
    @Override
    public DataBufferController rawData()
    {
        return dataBuffer;
    }
    @Override
    public int rawDataOffset()
    {
        return startOffset;
    }




    /**
     * @return the -1 if not reached yet, otherwise the end of the frame offset exclusive
     */
    @Override
    public int endFrameOffset()
    {
        return afterOffset(fullMessageSize);
    }

    /**
     * @param field looking for
     * @return relative starting byte index of field
     */
    @Override
    public int byteIndex(HTTPWSProto.WSFrameField field)
    {
        switch (field)
        {
            case FIN:
                return  HTTPWSProto.WSFrameField.FIN.byteIndex;
            case RSV1:
                return  HTTPWSProto.WSFrameField.RSV1.byteIndex;
            case RSV2:
                return  HTTPWSProto.WSFrameField.RSV2.byteIndex;
            case RSV3:
                return  HTTPWSProto.WSFrameField.RSV3.byteIndex;
            case OP_CODE:
                return  HTTPWSProto.WSFrameField.OP_CODE.byteIndex;
            case MASK_BIT:
                return  HTTPWSProto.WSFrameField.MASK_BIT.byteIndex;
            case DATA_LENGTH:
                if (dataLength() < 126 )
                    return HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex;
                else
                    return HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex + 1;
            case MASKING_KEY:
                if(isMasked())
                {
                    int dataLength = dataLength();
                    if (dataLength < 126)
                        return HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex + 1;

                    return HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex + 3;
                }
                return -1;
            case DATA:
                if (isMasked())
                    return byteIndex(HTTPWSProto.WSFrameField.MASKING_KEY) + 4;

                int dataLength = dataLength();
                if (dataLength < 126)
                    return HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex + 1;

                return HTTPWSProto.WSFrameField.DATA_LENGTH.byteIndex + 3;
            default:
                throw new IllegalArgumentException("Invalid field " + field);
        }
    }

    /**
     * @return status
     */
    @Override
    public MessageStatus status()
    {
        return status;
    }

    /**
     * @return the expected size of frame including header and data, -1 buffer incomplete
     */
    @Override
    public synchronized int frameSize()
    {

        if(fullMessageSize == -1 &&
                status != MessageStatus.INVALID &&
                dataBuffer.size() > afterOffset(HTTPWSProto.WSFrameField.MASK_BIT.byteIndex + 1))
        {
            int byteCounter = HTTPWSProto.WSFrameField.MASK_BIT.byteIndex + 1;
            // 2 bytes minimum size of a frame
            int dataLengthByte = dataLengthByte();

            if(isMasked())
            {
                // we have 4 byte of mask
                byteCounter +=4;
            }

            if (dataLengthByte < 126)
            {

                byteCounter += dataLengthByte;
                if (dataBuffer.size() >= afterOffset(byteCounter))
                {
                    status = MessageStatus.COMPLETE;
                    fullMessageSize = byteCounter;
                }
                else
                {
                    status = MessageStatus.PARTIAL;
                }
            }
            else if (dataLengthByte == 126)
            {

                if (dataBuffer.size() >= afterOffset(byteCounter + 2))
                {
                    // 2 bytes for the 16 bit length
                    byteCounter += (2 + dataLength());
                    if(dataBuffer.size() >= byteCounter)
                    {
                        fullMessageSize = byteCounter;
                        status = MessageStatus.COMPLETE;
                    }
                    else
                    {
                        status = MessageStatus.PARTIAL;
                    }
                }
            }
            else if (dataLengthByte == 127)
            {
                status = MessageStatus.INVALID;
                throw new UnsupportedOperationException("32 bit data length not supported, only 16 bit or < 126");
            }
        }


        return fullMessageSize;
    }

    private int afterOffset(int val)
    {
        return val > 0 ? startOffset + val : -1;
    }

}
