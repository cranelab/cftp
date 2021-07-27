package net.engio.mbassy.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ReflectionUtils {
  public static Method[] getMethods(IPredicate<Method> condition, Class<?> target) {
    ArrayList<Method> methods = new ArrayList<Method>();
    getMethods(condition, target, methods);
    Method[] array = new Method[methods.size()];
    methods.toArray(array);
    return array;
  }
  
  public static void getMethods(IPredicate<Method> condition, Class<?> target, ArrayList<Method> methods) {
    try {
      for (Method method : target.getDeclaredMethods()) {
        if (condition.apply(method))
          methods.add(method); 
      } 
    } catch (Exception exception) {}
    if (!target.equals(Object.class))
      getMethods(condition, target.getSuperclass(), methods); 
  }
  
  public static Method getOverridingMethod(Method overridingMethod, Class subclass) {
    Class current = subclass;
    while (!current.equals(overridingMethod.getDeclaringClass())) {
      try {
        return current.getDeclaredMethod(overridingMethod.getName(), overridingMethod.getParameterTypes());
      } catch (NoSuchMethodException e) {
        current = current.getSuperclass();
      } 
    } 
    return null;
  }
  
  public static Class[] getSuperTypes(Class<?> from) {
    ArrayList<Class<?>> superclasses = new ArrayList<Class<?>>();
    collectInterfaces(from, (Collection)superclasses);
    while (!from.equals(Object.class) && !from.isInterface()) {
      superclasses.add(from.getSuperclass());
      from = from.getSuperclass();
      collectInterfaces(from, (Collection)superclasses);
    } 
    Class[] classes = new Class[superclasses.size()];
    superclasses.toArray((Class<?>[][])classes);
    return classes;
  }
  
  public static void collectInterfaces(Class from, Collection<Class<?>> accumulator) {
    for (Class<?> intface : from.getInterfaces()) {
      accumulator.add(intface);
      collectInterfaces(intface, accumulator);
    } 
  }
  
  public static boolean containsOverridingMethod(Method[] allMethods, Method methodToCheck) {
    int length = allMethods.length;
    for (int i = 0; i < length; i++) {
      Method method = allMethods[i];
      if (isOverriddenBy(methodToCheck, method))
        return true; 
    } 
    return false;
  }
  
  private static <A extends Annotation> A getAnnotation(AnnotatedElement from, Class<A> annotationType, Set<AnnotatedElement> visited) {
    if (visited.contains(from))
      return null; 
    visited.add(from);
    A ann = from.getAnnotation(annotationType);
    if (ann != null)
      return ann; 
    for (Annotation metaAnn : from.getAnnotations()) {
      ann = getAnnotation(metaAnn.annotationType(), annotationType, visited);
      if (ann != null)
        return ann; 
    } 
    return null;
  }
  
  public static <A extends Annotation> A getAnnotation(AnnotatedElement from, Class<A> annotationType) {
    return getAnnotation(from, annotationType, new HashSet<AnnotatedElement>());
  }
  
  private static boolean isOverriddenBy(Method superclassMethod, Method subclassMethod) {
    if (superclassMethod.getDeclaringClass().equals(subclassMethod.getDeclaringClass()) || 
      !superclassMethod.getDeclaringClass().isAssignableFrom(subclassMethod.getDeclaringClass()) || 
      !superclassMethod.getName().equals(subclassMethod.getName()))
      return false; 
    Class[] superClassMethodParameters = superclassMethod.getParameterTypes();
    Class[] subClassMethodParameters = subclassMethod.getParameterTypes();
    for (int i = 0; i < subClassMethodParameters.length; i++) {
      if (!superClassMethodParameters[i].equals(subClassMethodParameters[i]))
        return false; 
    } 
    return true;
  }
}
