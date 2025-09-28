package org.zoxweb.shared.util;

public interface DataCodec<I,O>
    extends DataEncoder<I,O>, DataDecoder<O, I>
{
}
