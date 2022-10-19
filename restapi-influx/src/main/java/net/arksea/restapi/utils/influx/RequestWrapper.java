package net.arksea.restapi.utils.influx;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;

/**
 * Create by xiaohaixing on 2020/6/9
 */
public class RequestWrapper extends HttpServletRequestWrapper {
    private final byte[] bodyBytes;
    public RequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        try(InputStream reqIn = request.getInputStream()) {
            if (reqIn == null) {
                bodyBytes = new byte[0];
            } else {
                try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    int n;
                    byte[] byteBuffer = new byte[128];
                    while ((n = reqIn.read(byteBuffer)) > 0) {
                        out.write(byteBuffer, 0, n);
                    }
                    out.flush();
                    bodyBytes = out.toByteArray();
                }
            }
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bodyInputStream = new ByteArrayInputStream(bodyBytes);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                //do nothing
            }

            @Override
            public int read() {
                return bodyInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public String getBody() throws UnsupportedEncodingException {
        return new String(bodyBytes, this.getCharacterEncoding());
    }

    public String getBody(Charset charset) {
        return new String(bodyBytes, charset);
    }
}
