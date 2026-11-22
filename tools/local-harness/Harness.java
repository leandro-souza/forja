import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Servidor local de testes: executa os controllers e servicos reais sobre repositorios em memoria. */
public class Harness {
  static final Map<Class<?>, Object> beans = new HashMap<>();
  static final ObjectMapper mapper = new ObjectMapper();
  record Route(String method, Pattern pattern, List<String> vars, Object bean, Method m) {}
  static final List<Route> routes = new ArrayList<>();

  @SuppressWarnings("unchecked")
  static Object repo(Class<?> iface) {
    Class<?> entity = (Class<?>) ((ParameterizedType) iface.getGenericInterfaces()[0]).getActualTypeArguments()[0];
    List<Object> store = new ArrayList<>(); long[] seq = {0};
    return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (p, m, a) -> {
      String n = m.getName();
      Field idf = entity.getDeclaredField("id"); idf.setAccessible(true);
      switch (n) {
        case "save": if (idf.get(a[0]) == null) { idf.set(a[0], ++seq[0]); store.add(a[0]); } else { Object id = idf.get(a[0]); store.replaceAll(x -> { try { return idf.get(x).equals(id) ? a[0] : x; } catch (Exception e) { throw new RuntimeException(e); } }); } return a[0];
        case "findById": for (Object o : store) if (idf.get(o).equals(a[0])) return Optional.of(o); return Optional.empty();
        case "findAll": return new ArrayList<>(store);
        case "delete": store.remove(a[0]); return null;
        case "deleteAll": for (Object o : (Iterable<Object>) a[0]) store.remove(o); return null;
        case "count": return (long) store.size();
        case "flush": return null;
        case "hashCode": return System.identityHashCode(p);
        case "equals": return p == a[0];
        case "toString": return iface.getName();
      }
      String rest = n.substring("findBy".length()); String order = null;
      int oi = rest.indexOf("OrderBy"); if (oi >= 0) { order = rest.substring(oi + 7); rest = rest.substring(0, oi); }
      String[] props = rest.split("And"); List<Object> out = new ArrayList<>();
      for (Object o : store) { boolean ok = true; for (int i = 0; i < props.length; i++) { Field f = entity.getDeclaredField(Character.toLowerCase(props[i].charAt(0)) + props[i].substring(1)); f.setAccessible(true); ok &= Objects.equals(f.get(o), a[i]); } if (ok) out.add(o); }
      if (order != null) { List<Field> keys = new ArrayList<>(); for (String k : order.split("Asc")) { if (k.isEmpty()) continue; Field f = entity.getDeclaredField(Character.toLowerCase(k.charAt(0)) + k.substring(1)); f.setAccessible(true); keys.add(f); }
        out.sort((x, y) -> { try { for (Field f : keys) { int c = ((Comparable<Object>) f.get(x)).compareTo(f.get(y)); if (c != 0) return c; } return 0; } catch (Exception e) { throw new RuntimeException(e); } }); }
      return m.getReturnType() == Optional.class ? out.stream().findFirst() : out;
    });
  }

  static Object bean(Class<?> c) throws Exception {
    if (beans.containsKey(c)) return beans.get(c);
    Object o;
    if (c == ObjectMapper.class) o = mapper;
    else if (c.isInterface()) o = repo(c);
    else { Constructor<?> k = c.getDeclaredConstructors()[0]; Object[] args = new Object[k.getParameterCount()]; for (int i = 0; i < args.length; i++) args[i] = bean(k.getParameterTypes()[i]); o = k.newInstance(args);
      for (Field f : c.getDeclaredFields()) { var v = f.getAnnotation(org.springframework.beans.factory.annotation.Value.class); if (v == null) continue; String[] kv = v.value().substring(2, v.value().length() - 1).split(":", 2); String val = System.getenv().getOrDefault(kv[0], kv.length > 1 ? kv[1] : ""); f.setAccessible(true); f.set(o, f.getType() == boolean.class ? (Object) Boolean.parseBoolean(val) : val); } }
    beans.put(c, o); return o;
  }

  static void register(Class<?> c) throws Exception {
    Object b = bean(c); String prefix = c.getAnnotation(RequestMapping.class).value()[0];
    for (Method m : c.getDeclaredMethods()) {
      String http = null; String[] paths = null;
      if (m.isAnnotationPresent(GetMapping.class)) { http = "GET"; paths = m.getAnnotation(GetMapping.class).value(); }
      if (m.isAnnotationPresent(PostMapping.class)) { http = "POST"; paths = m.getAnnotation(PostMapping.class).value(); }
      if (m.isAnnotationPresent(PutMapping.class)) { http = "PUT"; paths = m.getAnnotation(PutMapping.class).value(); }
      if (m.isAnnotationPresent(DeleteMapping.class)) { http = "DELETE"; paths = m.getAnnotation(DeleteMapping.class).value(); }
      if (http == null) continue;
      String path = prefix + (paths.length > 0 ? paths[0] : ""); List<String> vars = new ArrayList<>(); Matcher mt = Pattern.compile("\\{(\\w+)}").matcher(path); while (mt.find()) vars.add(mt.group(1));
      routes.add(new Route(http, Pattern.compile(path.replaceAll("\\{\\w+}", "([^/]+)")), vars, b, m));
    }
  }

  public static void main(String[] args) throws Exception {
    for (String c : List.of("AuthController", "ProfileController", "TrainController", "NutritionController")) register(Class.forName("br.com.forja.web." + c));
    Object tokens = bean(Class.forName("br.com.forja.domain.Repositories$Tokens"));
    Path dist = Path.of(args.length > 1 ? args[1] : "dist");
    HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(args[0])), 0);
    server.createContext("/", ex -> {
      String path = ex.getRequestURI().getPath(); int status = 200; byte[] body; String type = "application/json; charset=utf-8";
      try {
        if (!path.startsWith("/api/")) {
          Path f = dist.resolve(path.equals("/") ? "index.html" : path.substring(1)).normalize();
          if (!f.startsWith(dist) || !Files.isRegularFile(f)) f = dist.resolve("index.html");
          String nm = f.toString(); type = nm.endsWith(".html") ? "text/html; charset=utf-8" : nm.endsWith(".js") ? "application/javascript" : nm.endsWith(".css") ? "text/css" : nm.endsWith(".json") || nm.endsWith(".webmanifest") ? "application/json" : nm.endsWith(".png") ? "image/png" : nm.endsWith(".jpg") ? "image/jpeg" : nm.endsWith(".svg") ? "image/svg+xml" : nm.endsWith(".woff2") ? "font/woff2" : "application/octet-stream";
          body = Files.readAllBytes(f);
        } else {
          Long userId = null;
          if (!path.startsWith("/api/auth/") && !path.equals("/api/health")) {
            String h = ex.getRequestHeaders().getFirst("Authorization");
            Optional<?> t = h == null ? Optional.empty() : (Optional<?>) tokens.getClass().getMethod("findByToken", String.class).invoke(tokens, h.replace("Bearer ", ""));
            if (t.isEmpty()) throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre novamente.");
            userId = (Long) t.get().getClass().getMethod("getUserId").invoke(t.get());
          }
          Map<String, String> q = new HashMap<>(); String raw = ex.getRequestURI().getRawQuery(); if (raw != null) for (String kv : raw.split("&")) { String[] p = kv.split("=", 2); q.put(p[0], p.length > 1 ? java.net.URLDecoder.decode(p[1], StandardCharsets.UTF_8) : ""); }
          String reqBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); Object result = null; boolean found = false;
          for (Route r : routes) { Matcher mt = r.pattern().matcher(path); if (!r.method().equals(ex.getRequestMethod()) || !mt.matches()) continue; found = true;
            Parameter[] ps = r.m().getParameters(); Object[] a = new Object[ps.length];
            for (int i = 0; i < ps.length; i++) { Parameter p = ps[i];
              if (p.isAnnotationPresent(RequestAttribute.class)) a[i] = userId;
              else if (p.isAnnotationPresent(RequestBody.class)) a[i] = ObjectMapper.convert(ObjectMapper.parse(reqBody), p.getParameterizedType());
              else if (p.isAnnotationPresent(PathVariable.class)) a[i] = Long.parseLong(mt.group(r.vars().indexOf(p.getName()) + 1));
              else if (p.isAnnotationPresent(RequestParam.class)) { String v = q.get(p.getName()); if (v == null && !p.getAnnotation(RequestParam.class).defaultValue().isEmpty()) v = p.getAnnotation(RequestParam.class).defaultValue(); a[i] = v == null ? null : p.getType() == LocalDate.class ? (Object) LocalDate.parse(v) : v; } }
            try { result = r.m().invoke(r.bean(), a); } catch (InvocationTargetException ite) { if (ite.getCause() instanceof RuntimeException re) throw re; throw ite; }
            break; }
          if (!found) throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Rota inexistente: " + path);
          body = (result == null ? "" : mapper.writeValueAsString(result)).getBytes(StandardCharsets.UTF_8);
        }
      } catch (ResponseStatusException e) { status = e.status.code; body = ("{\"message\":" + mapper.writeValueAsString(e.getMessage()) + "}").getBytes(StandardCharsets.UTF_8);
      } catch (Exception e) { e.printStackTrace(); status = 500; body = ("{\"message\":" + mapper.writeValueAsString(String.valueOf(e)) + "}").getBytes(StandardCharsets.UTF_8); }
      ex.getResponseHeaders().set("Content-Type", type); ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length); if (body.length > 0) ex.getResponseBody().write(body); ex.close();
    });
    server.start(); System.out.println("harness on " + args[0]);
  }
}
