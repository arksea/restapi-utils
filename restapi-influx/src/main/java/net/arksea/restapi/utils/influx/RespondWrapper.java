package net.arksea.restapi.utils.influx;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Create by xiaohaixing on 2020/6/10
 */
public class RespondWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private ServletOutputStream out;
    private PrintWriter writer;

    public RespondWrapper(HttpServletResponse resp) throws IOException {
        super(resp);
        this.out = new ServletOutputStream() {
            @Override
            public void write(int b) {
                buffer.write(b);
            }
            @Override
            public void write(byte[] b) {
                buffer.write(b, 0, b.length);
            }
            @Override
            public boolean isReady() {
                return false;
            }
            @Override
            public void setWriteListener(WriteListener writeListener) {
                //do nothing
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
        out.flush();
        writer.flush();
    }

    @Override
    public void reset() {
        buffer.reset();
    }

    private byte[] getResponseData() throws IOException {
        flushBuffer();
        return buffer.toByteArray();
    }

    public String writeBody() throws IOException {
        byte[] body = getResponseData();
        try(PrintWriter w = super.getWriter()) {
            w.print(new String(body, this.getCharacterEncoding()));
            w.flush();
        }
        return new String(body, this.getCharacterEncoding());
    }
}
