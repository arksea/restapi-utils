package net.arksea.restapi.utils.influx;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.Charset;

/**
 * Create by xiaohaixing on 2020/6/10
 */
public class RespondWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final ServletOutputStream out;
    private final PrintWriter writer;
    public RespondWrapper(HttpServletResponse resp) throws IOException {
        super(resp);
        final ServletOutputStream origin = super.getOutputStream();
        this.out = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                buffer.write(b);
                origin.write(b);
            }
            @Override
            public void write(byte[] b) throws IOException {
                buffer.write(b, 0, b.length);
                origin.write(b, 0, b.length);
            }
            @Override
            public boolean isReady() {
                return origin.isReady();
            }
            @Override
            public void setWriteListener(WriteListener writeListener) {
                origin.setWriteListener(writeListener);
            }
        };
        writer = new PrintWriter(new OutputStreamWriter(out, this.getCharacterEncoding()));
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return out;
    }

    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
        out.flush();
    }

    @Override
    public void reset() {
        buffer.reset();
    }

    public String getRespondBody() {
        try {
            return buffer.toString(this.getCharacterEncoding());
        } catch (UnsupportedEncodingException ex) {
            return buffer.toString();
        }
    }

    public String getRespondBody(Charset charset) {
        try {
            return buffer.toString(charset.name());
        } catch (UnsupportedEncodingException ex) {
            return buffer.toString();
        }
    }
}
