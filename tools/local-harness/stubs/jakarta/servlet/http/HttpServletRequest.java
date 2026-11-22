package jakarta.servlet.http; public interface HttpServletRequest { String getRequestURI(); String getHeader(String n); void setAttribute(String n, Object v); }
