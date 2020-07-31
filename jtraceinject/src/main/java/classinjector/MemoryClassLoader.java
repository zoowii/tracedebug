package classinjector;

import java.util.concurrent.ConcurrentHashMap;

public class MemoryClassLoader extends ClassLoader {
    private final ConcurrentHashMap<String, byte[]> memoryClasses = new ConcurrentHashMap<>();

    public void addClass(String className, byte[] classBytes) {
        System.out.println("memory loader add class " + className);
        memoryClasses.put(className, classBytes);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println("memory loader find class " + name);
        byte[] classData = getClassData(name);
        if (classData == null) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw e;
            }
//            throw new ClassNotFoundException();
        } else {
            return defineClass(name, classData, 0, classData.length);
        }
    }

    private byte[] getClassData(String className) {
        byte[] classBytes = memoryClasses.get(className);
        if (classBytes == null) {
            return null;
        }
        return classBytes;
    }
}
