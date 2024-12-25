package io.github.gaoxingliang.dbupgrader.utils;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.jar.*;

public class ReflectionUtils {
    /**
     * The spring way ...
     *
     * @param packageName
     * @return
     * @throws IOException
     */
    public static List<Class> getClasses(String packageName) throws IOException, ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                findClassesInDir(new File(resource.getFile()), packageName, classes);
            } else if ("jar".equals(protocol)) {
                findClassesInJar(resource, path, packageName, classes);
            }
        }

        return classes;
    }

    private static void findClassesInDir(File directory, String packageName, List<Class> classes) throws ClassNotFoundException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isDirectory()) {
                    findClassesInDir(file, packageName + "." + fileName, classes);
                } else if (fileName.endsWith(".class")) {
                    String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                    classes.add(Class.forName(className));
                }
            }
        }
    }

    private static void findClassesInJar(URL resource, String path, String packageName, List<Class> classes) throws IOException,
            ClassNotFoundException {
        String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
        JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name()));

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            // 检查是否是指定包下的类文件
            if (name.startsWith(path) && name.endsWith(".class")) {
                // 转换为类名
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                classes.add(Class.forName(className));
            }
        }

        jar.close();
    }
}
