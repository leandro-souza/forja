package jakarta.servlet; import jakarta.servlet.http.*; public interface FilterChain { void doFilter(HttpServletRequest a, HttpServletResponse b) throws java.io.IOException, ServletException; }
