package graphql.kickstart.servlet.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BufferedHttpServletResponse extends HttpServletResponseWrapper {

  private BufferedOutputStream copier;
  private ServletOutputStream outputStream;
  private PrintWriter writer;
  private String errorMessage;

  public BufferedHttpServletResponse(HttpServletResponse response) {
    super(response);
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    errorMessage = msg;
    super.sendError(sc, msg);
  }

  @Override
  public void sendError(int sc) throws IOException {
    sendError(sc, null);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (writer != null) {
      throw new IllegalStateException("getWriter() has already been called on this response.");
    }

    if (outputStream == null) {
      outputStream = getResponse().getOutputStream();
      copier = new BufferedOutputStream(outputStream);
    }

    return copier;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (outputStream != null) {
      throw new IllegalStateException(
          "getOutputStream() has already been called on this response.");
    }

    if (writer == null) {
      copier = new BufferedOutputStream(getResponse().getOutputStream());
      writer =
          new PrintWriter(
              new OutputStreamWriter(copier, getResponse().getCharacterEncoding()), true);
    }

    return writer;
  }

  @Override
  public void flushBuffer() throws IOException {
    if (writer != null) {
      writer.flush();
    } else if (copier != null) {
      copier.flush();
    }
  }

  @Override
  public boolean isCommitted() {
    return false;
  }

  public void close() throws IOException {
    if (writer != null) {
      writer.close();
    } else if (copier != null) {
      copier.close();
    }
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public byte[] getContentAsByteArray() {
    if (copier != null) {
      return copier.toByteArray();
    } else {
      return new byte[0];
    }
  }

  private static final class BufferedOutputStream extends ServletOutputStream {

    private final OutputStream delegate;
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

    public BufferedOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    public void write(int b) throws IOException {
      buf.write(b);
      delegate.write(b);
    }

    @Override
    public void flush() throws IOException {
      buf.flush();
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      buf.close();
      delegate.close();
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      // write listener not supported
    }

    public byte[] toByteArray() {
      return buf.toByteArray();
    }
  }
}
