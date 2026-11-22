package com.fasterxml.jackson.databind;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
/** Mini implementacao para o ambiente de testes local (sem acesso ao Maven Central). */
public class ObjectMapper {
  public <T> T readValue(String s, Class<T> c) { return c.cast(convert(parse(s), c)); }
  public String writeValueAsString(Object o) { StringBuilder sb=new StringBuilder(); write(o,sb); return sb.toString(); }
  public JsonNode readTree(String s) { return new JsonNode(parse(s)); }
  public <T> T treeToValue(JsonNode n, Class<T> c) { return c.cast(convert(n.tree, c)); }

  public static void write(Object o, StringBuilder sb){
    if(o==null){sb.append("null");return;}
    if(o instanceof JsonNode n){write(n.tree,sb);return;}
    if(o instanceof Optional<?> op){write(op.orElse(null),sb);return;}
    if(o instanceof String||o instanceof Enum||o instanceof LocalDate||o instanceof Instant||o instanceof Character){ quote(o.toString(),sb);return;}
    if(o instanceof Number||o instanceof Boolean){sb.append(o);return;}
    if(o instanceof Map<?,?> m){sb.append('{');boolean f=true;for(var e:m.entrySet()){if(!f)sb.append(',');f=false;quote(String.valueOf(e.getKey()),sb);sb.append(':');write(e.getValue(),sb);}sb.append('}');return;}
    if(o instanceof Iterable<?> it){sb.append('[');boolean f=true;for(Object x:it){if(!f)sb.append(',');f=false;write(x,sb);}sb.append(']');return;}
    if(o.getClass().isArray()){sb.append('[');for(int i=0;i<Array.getLength(o);i++){if(i>0)sb.append(',');write(Array.get(o,i),sb);}sb.append(']');return;}
    try{
      sb.append('{');boolean f=true;
      if(o.getClass().isRecord()){ for(RecordComponent rc:o.getClass().getRecordComponents()){ if(!f)sb.append(',');f=false;quote(rc.getName(),sb);sb.append(':'); Method m=rc.getAccessor(); m.setAccessible(true); write(m.invoke(o),sb);} }
      else { for(Field fd:o.getClass().getDeclaredFields()){ if(Modifier.isStatic(fd.getModifiers()))continue; fd.setAccessible(true); if(!f)sb.append(',');f=false;quote(fd.getName(),sb);sb.append(':');write(fd.get(o),sb);} }
      sb.append('}');
    }catch(Exception e){throw new RuntimeException(e);}
  }
  static void quote(String s,StringBuilder sb){sb.append('"');for(char c:s.toCharArray()){switch(c){case '"'->sb.append("\\\"");case '\\'->sb.append("\\\\");case '\n'->sb.append("\\n");case '\r'->sb.append("\\r");case '\t'->sb.append("\\t");default->{if(c<32)sb.append(String.format("\\u%04x",(int)c));else sb.append(c);}}}sb.append('"');}

  public static Object convert(Object v, Type t){
    try{
      if(v==null) return null;
      if(t instanceof ParameterizedType pt){ Class<?> raw=(Class<?>)pt.getRawType();
        if(List.class.isAssignableFrom(raw)){List<Object> l=new ArrayList<>();for(Object x:(List<?>)v)l.add(convert(x,pt.getActualTypeArguments()[0]));return l;}
        if(Map.class.isAssignableFrom(raw)){Map<String,Object> m=new LinkedHashMap<>();for(var e:((Map<?,?>)v).entrySet())m.put((String)e.getKey(),convert(e.getValue(),pt.getActualTypeArguments()[1]));return m;}
        return convert(v,raw);}
      Class<?> c=(Class<?>)t;
      if(c==Object.class) return v;
      if(c==JsonNode.class) return new JsonNode(v);
      if(c==String.class) return v.toString();
      if(c==Integer.class||c==int.class) return ((Number)v).intValue();
      if(c==Long.class||c==long.class) return ((Number)v).longValue();
      if(c==Double.class||c==double.class) return ((Number)v).doubleValue();
      if(c==Boolean.class||c==boolean.class) return v;
      if(c==LocalDate.class) return LocalDate.parse((String)v);
      if(c==Instant.class) return Instant.parse((String)v);
      if(c.isEnum()){ for(Object k:c.getEnumConstants()) if(k.toString().equals(v)||((Enum<?>)k).name().equals(v)) return k; return null; }
      Map<?,?> m=(Map<?,?>)v;
      if(c.isRecord()){ RecordComponent[] rcs=c.getRecordComponents(); Object[] args=new Object[rcs.length]; Class<?>[] types=new Class<?>[rcs.length];
        for(int i=0;i<rcs.length;i++){types[i]=rcs[i].getType(); Object x=convert(m.get(rcs[i].getName()),rcs[i].getGenericType()); if(x==null&&types[i].isPrimitive()) x= types[i]==boolean.class?(Object)false: types[i]==double.class?(Object)0.0: types[i]==long.class?(Object)0L:(Object)0; args[i]=x;}
        Constructor<?> k=c.getDeclaredConstructor(types); k.setAccessible(true); return k.newInstance(args);}
      Object o=c.getDeclaredConstructor().newInstance();
      for(Field fd:c.getDeclaredFields()){ if(Modifier.isStatic(fd.getModifiers())||!m.containsKey(fd.getName()))continue; fd.setAccessible(true); fd.set(o,convert(m.get(fd.getName()),fd.getGenericType())); }
      return o;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}
  }

  public static Object parse(String s){ return new P(s).val(); }
  static class P{ final String s;int i=0;P(String s){this.s=s;}
    void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
    Object val(){ws();char c=s.charAt(i);
      if(c=='{'){i++;Map<String,Object> m=new LinkedHashMap<>();ws();if(s.charAt(i)=='}'){i++;return m;}while(true){ws();String k=str();ws();i++;m.put(k,val());ws();if(s.charAt(i++)=='}')return m;}}
      if(c=='['){i++;List<Object> l=new ArrayList<>();ws();if(s.charAt(i)==']'){i++;return l;}while(true){l.add(val());ws();if(s.charAt(i++)==']')return l;}}
      if(c=='"')return str();
      if(s.startsWith("true",i)){i+=4;return true;} if(s.startsWith("false",i)){i+=5;return false;} if(s.startsWith("null",i)){i+=4;return null;}
      int st=i;while(i<s.length()&&"+-0123456789.eE".indexOf(s.charAt(i))>=0)i++;String n=s.substring(st,i);
      if(n.contains(".")||n.contains("e")||n.contains("E"))return Double.parseDouble(n); long l=Long.parseLong(n); return (l>=Integer.MIN_VALUE&&l<=Integer.MAX_VALUE)?(Object)(int)l:(Object)l;}
    String str(){i++;StringBuilder sb=new StringBuilder();while(s.charAt(i)!='"'){char c=s.charAt(i++);if(c=='\\'){char e=s.charAt(i++);switch(e){case 'n'->sb.append('\n');case 't'->sb.append('\t');case 'r'->sb.append('\r');case 'b'->sb.append('\b');case 'f'->sb.append('\f');case 'u'->{sb.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;}default->sb.append(e);}}else sb.append(c);}i++;return sb.toString();}}
}
