package org.zoxweb.shared.io;

import java.io.IOException;
import java.io.OutputStream;

public interface WriteTo {

     /**
      * Write the whole content to output stream
      * @param out stream
      * @throws IOException in case of error
      */
     void writeTo(OutputStream out) throws IOException;
}
