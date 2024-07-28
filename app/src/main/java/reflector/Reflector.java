package reflector;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.google.common.util.concurrent.UncheckedExecutionException;
import orsc.OpenRSC;
import orsc.mudclient;

/**
 * Reflector is a master reflection class which the entire bot is built off of. It contains helper
 * functions to make reflection much less of a pain.
 *
 * @author Dvorak
 */
public class Reflector {

  final ClassLoader classLoader = this.getClass().getClassLoader();

  private final Cache<Map.Entry<Object, String>, Field> objectAndFieldNameToFieldCache =
      CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();

  private final Cache<Map.Entry<String, String>, Field> classAndFieldNameToFieldCache =
      CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();

  /**
   * Creates the RSC client object.
   *
   * @return OpenRSC object
   */
  public OpenRSC createClient() {
    try {
      Class<?> loadedMyClass = classLoader.loadClass("orsc.OpenRSC");

      // Create a new instance from the loaded class
      Constructor<?> constructor = loadedMyClass.getConstructor();
      Object myClassObject = constructor.newInstance();

      // Getting the target method from the loaded class and invoke it using its name
      Method method = loadedMyClass.getDeclaredMethod("createAndShowGUI");

      method.invoke(myClassObject);

      return (OpenRSC) myClassObject;

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Given the OpenRSC client, this function returns the `mudclient`
   *
   * @return mudclient
   */
  public mudclient getMud(OpenRSC client) {
    Field mud;
    try {
      mud = client.getClass().getSuperclass().getDeclaredField("mudclient");
      mud.setAccessible(true);
      return (mudclient) mud.get(null);
    } catch (IllegalArgumentException
        | IllegalAccessException
        | NoSuchFieldException
        | SecurityException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * This function will call `methodName` with `params` inside of `mud`.
   *
   * @param mud -- the mud object
   * @param methodName -- the method name as a case sensitive string
   * @param params -- parameters, as a list
   * @return the return value of the specified `methodName` function
   */
  public Object mudInvoker(mudclient mud, String methodName, Object... params) {

    try {

      if (params != null) {
        Class<?>[] types = new Class<?>[params.length];

        int i = 0;
        for (Object param : params) {
          if (param.getClass() == Integer.class) {
            types[i] = int.class;
          } else if (param.getClass() == Boolean.class) {
            types[i] = boolean.class;
          } else {
            types[i] = param.getClass();
          }

          i++;
        }

        Method method = mud.getClass().getDeclaredMethod(methodName, types);
        method.setAccessible(true);

        return method.invoke(mud, params);
      } else {
        Method method = mud.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);

        return method.invoke(mud);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * This function will grab the `member` object from the specified `className`.
   *
   * <p>*** THIS DOES NOT RECURSE DOWN INTO CHILD OBJECTS. SEE OTHER FUNCTIONS FOR THAT CAPABILITY.
   * ***
   *
   * @param className -- the EXACT name of the class, down to the period. case sensitive.
   * @param member -- the EXACT name of the member of the class. case sensitive.
   * @return the object requested, or null.
   */
  public Object getClassMember(String className, String member) {
    try {
      return getStaticFieldFromClassName(className, member).get(this);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * This function will grab the `member` object from the specified `obj`.
   *
   * <p>*** THIS DOES NOT RECURSE DOWN INTO CHILD OBJECTS. SEE OTHER FUNCTIONS FOR THAT CAPABILITY.
   * ***
   *
   * @param obj -- the object to retrieve data from.
   * @param member -- the EXACT name of the member of the class. case sensitive.
   * @return the object requested, or null.
   */
  public Object getObjectMember(Object obj, String member) {

    try {
      return getInstancedFieldFromObjectAndClass(obj, obj.getClass(), member).get(obj);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Retrieves the member from the given object. It WILL work on superclass objects.
   *
   * @param obj -- object to retrieve from
   * @param member -- exact name of the object to return
   * @return requested object, or null if it does not exist
   */
  public Object getObjectMemberFromSuperclass(Object obj, String member) {
    try {
      // TODO: note that this doesn't work when child class and super class share variable name
      // TODO: expose field lookups as methods
      Class<?> currentClass = obj.getClass();
      do {
        final Field foundField = getInstancedFieldFromObjectAndClass(obj, currentClass, member);
        if (foundField != null) {
          return foundField.get(obj);
        }
        currentClass = currentClass.getSuperclass();
      } while (currentClass != null);

    } catch (IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Sets the specified member inside of the object to the specified value.
   *
   * @param obj -- the object to modify
   * @param member -- the member of said object to modify
   * @param value -- the value to set obj.member to.
   */
  public void setObjectMember(Object obj, String member, Object value) {

    try {
      final Field field = getInstancedFieldFromObjectAndClass(obj, obj.getClass(), member);
      if (field != null) {
        field.set(obj, value);
      }
    } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  @Nullable private Field getInstancedFieldFromObjectAndClass(
      final Object object, final Class<?> clazz, final String fieldName) {
    // TODO: change object-level cache to also include clazz
    final Map.Entry<Object, String> cacheKey =
        new AbstractMap.SimpleImmutableEntry<>(object, fieldName);

    try {
      return objectAndFieldNameToFieldCache.get(
          cacheKey,
          () -> {
            try {
              final Field fieldInObj = clazz.getDeclaredField(fieldName);
              fieldInObj.setAccessible(true);
              return fieldInObj;
            } catch (final NoSuchFieldException e) {
              throw new RuntimeException(e);
            }
          });
    } catch (ExecutionException e) {
      if (e.getCause() instanceof NoSuchFieldException) {
        System.out.println("Suppressed no such field exception");
      } else {
        e.printStackTrace();
      }
    }

    return null;
  }

  @Nullable private Field getStaticFieldFromClassName(final String className, final String fieldName) {
    try {
      final Map.Entry<String, String> cacheKey =
          new AbstractMap.SimpleImmutableEntry<>(className, fieldName);

      return classAndFieldNameToFieldCache.get(
          cacheKey,
          () -> {
            try {
              // Create a new JavaClassLoader
              ClassLoader classLoader = this.getClass().getClassLoader();

              // Load the target class using its binary name
              Class<?> cli = classLoader.loadClass(className);

              Field mudclientField = cli.getDeclaredField(fieldName);
              mudclientField.setAccessible(true);
              return mudclientField;
            } catch (final ClassNotFoundException | NoSuchFieldException e) {
              throw new RuntimeException(e);
            }
          });
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }
}
