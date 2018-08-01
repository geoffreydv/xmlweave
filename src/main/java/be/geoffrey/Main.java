package be.geoffrey;

import be.geoffrey.kb.SourceClass;
import be.geoffrey.kb.SubjectKnowledgeBase;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.xml.bind.JAXB;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {

    private static final int DEFAULT_FILL_COUNT = 2;
    private static boolean print;

    public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        String action = args[0];
        print = args.length <= 5;

        if (action.equals("xml_example")) {

            String xjcDirectory = args[1];
            String className = args[2];
            String outputFile = args[3];
            String instructionFile = args[4];

            ClassLoader cl = ClassLoader.getSystemClassLoader();

            URL[] urls = ((URLClassLoader)cl).getURLs();

            System.out.println("CP ENTRIES");
            System.out.println("==========");
            for(URL url: urls){
                System.out.println(url.getFile());
            }

            Map<String, String> properties = new HashMap<>();

            String props = FileUtils.readFileToString(new File(instructionFile), StandardCharsets.UTF_8);
            if (!StringUtils.isBlank(props)) {

                String[] lines = props.split("\n");

                for (String line : lines) {
                    String[] parts = line.split("=");
                    properties.put(parts[0], parts[1]);
                }
            }

            String xmlFile = createXmlFile(xjcDirectory, className, outputFile, properties);
            print(xmlFile);

        } else if (action.equals("quick_compare")) {

            String wsdlDirOne = args[1];
            String wsdlDirTwo = args[2];
            String wsdlName = args[3];

            DifferenceReport report = new DifferenceReport(wsdlDirOne, wsdlDirTwo);
            report.init();
            report.diffCommonServices(wsdlName);

        }
    }

    private static String createXmlFile(String xjcOutputPath,
                                        String classToGenerateXmlFor,
                                        String outputFile, Map<String, String> properties) throws ClassNotFoundException, IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        SubjectKnowledgeBase kb = new SubjectKnowledgeBase();
        kb.build(Lists.newArrayList(xjcOutputPath));
        SourceClass sourceClass = kb.findClassByClassNameWithoutPackage(classToGenerateXmlFor);

        if (sourceClass == null) {
            throw new IllegalArgumentException("The requested class was not found: '" + classToGenerateXmlFor + "'");
        }

        Object xmlRepresentation = Class.forName(sourceClass.getFullyQualifiedClassName()).newInstance();

        fillAllFields(xmlRepresentation, kb, classToGenerateXmlFor, properties);

        StringWriter sw = new StringWriter();
        JAXB.marshal(xmlRepresentation, sw);

        FileUtils.writeStringToFile(new File(outputFile), sw.toString(), StandardCharsets.UTF_8);

        return sw.toString();
    }

    private static void fillAllFields(Object objectToFill,
                                      SubjectKnowledgeBase knowledgeBase,
                                      String currentPath,
                                      Map<String, String> properties) throws IllegalAccessException, ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        Class<?> classMetadata = objectToFill.getClass();

        String basePath = currentPath;

        for (Field field : FieldUtils.getAllFields(classMetadata)) {

            currentPath = basePath + "." + field.getName();

//            System.out.println(basePath);

            boolean fillNull = false;
            if (properties.containsKey(currentPath)) {
                if (properties.get(currentPath).contains("FILL_NULL")) {
                    fillNull = true;
                }
            }

            if (!fillNull) {
                fillValueOfField(objectToFill, knowledgeBase, currentPath, properties, field);
            }
        }
    }

    private static void fillValueOfField(Object objectToFill,
                                         SubjectKnowledgeBase knowledgeBase,
                                         String currentPath,
                                         Map<String, String> properties,
                                         Field field) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        if (!Modifier.isPrivate(field.getModifiers())) {
            field.setAccessible(true);

            Class<?> classOfField = field.getType();

            if (String.class.isAssignableFrom(classOfField)) {

                field.set(objectToFill, "stringstringstring");

            } else if (classOfField.getName().equals("int")
                    || Integer.class.isAssignableFrom(classOfField)) {

                field.set(objectToFill, 999);

            } else if (classOfField.getName().equals("boolean")
                    || Boolean.class.isAssignableFrom(classOfField)) {

                field.set(objectToFill, true);

            } else if (BigDecimal.class.isAssignableFrom(classOfField)) {

                field.set(objectToFill, new BigDecimal("555.55555"));

            } else if (classOfField.isEnum()) {

                Method method = classOfField.getDeclaredMethod("values");
                Object[] enumValues = (Object[]) method.invoke(field); // Call the values method to get enum results
                field.set(objectToFill, enumValues[0]);

            } else if (Set.class.isAssignableFrom(classOfField)) {

                Set<Object> setToFill = new HashSet<>();

                Class type = ((Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);

                if (String.class.isAssignableFrom(type)) {
                    setToFill.add("stringstringstringstring");
                } else {
                    for (int i = 0; i < DEFAULT_FILL_COUNT; i++) {
                        Object itemInList = type.newInstance();
                        fillAllFields(itemInList, knowledgeBase, currentPath + "[" + i + "]", properties);
                        setToFill.add(itemInList);
                    }
                }

                field.set(objectToFill, setToFill);

            } else if (List.class.isAssignableFrom(classOfField)) {

                List<Object> listToFill = new ArrayList<>();

                Class type = ((Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);

                // Maak een item op basis van het type

                if (String.class.isAssignableFrom(type)) {
                    listToFill.add("stringstringstringstring");
                } else {
                    for (int i = 0; i < DEFAULT_FILL_COUNT; i++) {
                        Object itemInList = type.newInstance();
                        fillAllFields(itemInList, knowledgeBase, currentPath + "[" + i + "]", properties);
                        listToFill.add(itemInList);
                    }
                }

                field.set(objectToFill, listToFill);

            } else if (Modifier.isAbstract(classOfField.getModifiers())) {

                SourceClass abstractInfo = knowledgeBase.findClass(classOfField.getName());
                if (abstractInfo == null) {
                    throw new IllegalArgumentException("Base class not found:" + classOfField.getName());
                }
                List<String> availableClassNames = abstractInfo.getAvailableImplementationClassNames();

                if (!availableClassNames.isEmpty()) {
                    Object instanceOfField = Class.forName(availableClassNames.get(0)).newInstance();

                    fillAllFields(instanceOfField, knowledgeBase, currentPath, properties);
                    field.set(objectToFill, instanceOfField);
                } else {
                    print("No superclass info available");
                }
            } else {

//                print("Filling field of custom type: " + classOfField.getName());

                try {
                    Object instanceOfField = classOfField.newInstance();
                    fillAllFields(instanceOfField, knowledgeBase, currentPath, properties);
                    field.set(objectToFill, instanceOfField);
                } catch (Exception ex) {
                    print("Error occurred on field " + currentPath);
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void print(Object message) {
        if (print) {
            System.out.println(message);
        }
    }
}