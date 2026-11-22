package jakarta.servlet.http; public interface HttpServletResponse { void setStatus(int s); void setContentType(String s); java.io.PrintWriter getWriter() throws java.io.IOException; }
