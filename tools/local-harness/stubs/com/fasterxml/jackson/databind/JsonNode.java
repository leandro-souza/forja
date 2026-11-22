package com.fasterxml.jackson.databind;
import java.util.*;
public class JsonNode implements Iterable<JsonNode> {
  public final Object tree;
  public JsonNode(Object t){tree=t;}
  public boolean has(String f){ return tree instanceof Map<?,?> m && m.containsKey(f); }
  public JsonNode get(String f){ return new JsonNode(((Map<?,?>)tree).get(f)); }
  public Iterator<JsonNode> iterator(){ List<JsonNode> l=new ArrayList<>(); if(tree instanceof List<?> x) for(Object o:x) l.add(new JsonNode(o)); return l.iterator(); }
}
